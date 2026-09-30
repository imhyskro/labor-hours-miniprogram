package com.labor.management.service;

import com.labor.management.common.ResultCode;
import com.labor.management.dto.ApprovalDecisionDTO;
import com.labor.management.dto.AttendanceBatchScoreDTO;
import com.labor.management.dto.AttendanceRecordSaveDTO;
import com.labor.management.dto.ModificationRequestCreateDTO;
import com.labor.management.exception.BusinessException;
import com.labor.management.security.LoginUser;
import com.labor.management.vo.ApprovalActionVO;
import com.labor.management.vo.AttendanceRecordVO;
import com.labor.management.vo.AttendanceSessionVO;
import com.labor.management.vo.ChangeRequestVO;
import com.labor.management.vo.ClassVO;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * V2 考勤、课次成绩和修改审批服务。
 *
 * <p>本服务只访问 V2 表，不再依赖旧 classes、attendance_session 表。
 * teachingGroupId 为小程序接口中的 classId。</p>
 */
@Service
@RequiredArgsConstructor
public class V2AttendanceService {

    private static final Set<String> REQUEST_STATUSES = Set.of(
            "PENDING", "APPROVED", "REJECTED", "USED", "CANCELLED", "EXPIRED");

    private final JdbcTemplate jdbc;

    public List<ClassVO> listAccessibleGroups() {
        CurrentUser user = currentUser();
        StringBuilder sql = new StringBuilder("""
                SELECT tg.id, tg.class_code, tg.company_id, tg.company_name_snapshot,
                       tg.week, tg.status, tg.created_at, at.academic_year,
                       MIN(cs.start_period) AS start_period,
                       MAX(cs.end_period) AS end_period,
                       COUNT(DISTINCT CASE WHEN e.status='ACTIVE' THEN e.id END) AS student_count
                FROM teaching_group tg
                JOIN academic_term at ON at.id=tg.term_id
                LEFT JOIN course_session cs ON cs.teaching_group_id=tg.id
                LEFT JOIN student_course_enrollment e ON e.teaching_group_id=tg.id
                """);
        List<Object> args = new ArrayList<>();
        if (user.admin()) {
            sql.append(" WHERE 1=1 ");
        } else if (user.teacher()) {
            sql.append(" WHERE tg.current_teacher_user_id=? ");
            args.add(user.id());
        } else {
            sql.append("""
                     JOIN assistant_group_assignment aga
                       ON aga.teaching_group_id=tg.id AND aga.unassigned_at IS NULL
                     JOIN assistant_profile ap
                       ON ap.id=aga.assistant_profile_id AND ap.status='ACTIVE'
                    WHERE ap.user_id=?
                    """);
            args.add(user.id());
        }
        sql.append("""
                 GROUP BY tg.id, tg.class_code, tg.company_id, tg.company_name_snapshot,
                          tg.week, tg.status, tg.created_at, at.academic_year
                 ORDER BY tg.week, tg.class_code
                """);
        return jdbc.query(sql.toString(), (rs, rowNum) -> {
            ClassVO vo = new ClassVO();
            vo.setId(rs.getLong("id"));
            vo.setClassCode(rs.getString("class_code"));
            vo.setCompanyId(nullableLong(rs, "company_id"));
            vo.setCompanyName(rs.getString("company_name_snapshot"));
            vo.setWeek(nullableInt(rs, "week"));
            vo.setStartSession(nullableInt(rs, "start_period"));
            vo.setEndSession(nullableInt(rs, "end_period"));
            vo.setClassName(rs.getString("company_name_snapshot") + "-" + rs.getString("class_code"));
            vo.setAcademicYear(rs.getString("academic_year"));
            vo.setStatus("ARCHIVED".equals(rs.getString("status")) ? 0 : 1);
            vo.setStudentCount(rs.getLong("student_count"));
            vo.setCreatedAt(dateTime(rs, "created_at"));
            return vo;
        }, args.toArray());
    }

    public List<AttendanceSessionVO> listSessions(Long teachingGroupId) {
        checkGroupAccess(teachingGroupId);
        return jdbc.query("""
                SELECT cs.*, tg.week, tg.company_name_snapshot AS group_company,
                       tg.class_code AS group_class_code
                  FROM course_session cs
                  JOIN teaching_group tg ON tg.id=cs.teaching_group_id
                 WHERE cs.teaching_group_id=?
                 ORDER BY cs.session_date, cs.start_period, cs.id
                """, (rs, rowNum) -> {
            AttendanceSessionVO vo = new AttendanceSessionVO();
            vo.setId(rs.getLong("id"));
            vo.setClassId(rs.getLong("teaching_group_id"));
            vo.setClassName(rs.getString("group_company") + "-" + rs.getString("group_class_code"));
            Integer week = nullableInt(rs, "week");
            vo.setWeekNo(week == null ? rowNum + 1 : week);
            vo.setWeekLabel("周" + vo.getWeekNo());
            vo.setSessionDate(rs.getDate("session_date").toLocalDate());
            vo.setIsLastSession(0);
            vo.setStatusCode(rs.getString("status"));
            vo.setScoreDeadlineAt(dateTime(rs, "score_deadline_at"));
            SessionWindow window = evaluateWindow(vo.getSessionDate(), vo.getScoreDeadlineAt(), vo.getStatusCode());
            vo.setEditable(window.editable());
            vo.setLockReason(window.reason());
            vo.setStatus(window.editable() ? 1 : 0);
            vo.setSealDate(vo.getScoreDeadlineAt() == null ? null : vo.getScoreDeadlineAt().toLocalDate());
            vo.setSealDays(readIntConfig("score.edit_deadline_days", 10));
            vo.setCreatedBy(nullableLong(rs, "created_by_user_id"));
            vo.setCreatedAt(dateTime(rs, "created_at"));
            vo.setUpdatedAt(dateTime(rs, "updated_at"));
            vo.setUnscoredCount(countUnscored(vo.getId(), window.editable()));
            return vo;
        }, teachingGroupId);
    }

    public List<AttendanceRecordVO> listRecords(Long sessionId) {
        SessionContext session = requireSession(sessionId);
        checkGroupAccess(session.groupId());
        CurrentUser user = currentUser();
        SessionWindow window = evaluateWindow(session.sessionDate(), session.deadline(), session.status());
        return jdbc.query("""
                SELECT e.id AS enrollment_id, e.student_id, e.student_no_snapshot,
                       e.student_name_snapshot, e.in_class_no,
                       CASE WHEN ap.id IS NULL THEN 0 ELSE 1 END AS is_assistant,
                       ar.id AS attendance_id, ar.attendance_type, ar.recorded_by_user_id,
                       sr.id AS score_record_id, sr.score_value, sr.score_mark, sr.remark,
                       sr.revision_count, sr.locked, sr.updated_at,
                       (SELECT cr.id FROM change_request cr
                         WHERE cr.target_table='score_record' AND cr.target_id=sr.id
                           AND cr.applicant_user_id=? AND cr.status IN ('PENDING','APPROVED')
                           AND (cr.status='PENDING' OR cr.edit_window_expires_at>=CURRENT_TIMESTAMP(3))
                         ORDER BY cr.created_at DESC LIMIT 1) AS current_request_id,
                       (SELECT cr.status FROM change_request cr
                         WHERE cr.target_table='score_record' AND cr.target_id=sr.id
                           AND cr.applicant_user_id=? AND cr.status IN ('PENDING','APPROVED')
                           AND (cr.status='PENDING' OR cr.edit_window_expires_at>=CURRENT_TIMESTAMP(3))
                         ORDER BY cr.created_at DESC LIMIT 1) AS current_request_status
                  FROM student_course_enrollment e
                  LEFT JOIN assistant_profile ap
                    ON ap.student_id=e.student_id AND ap.status='ACTIVE'
                  LEFT JOIN attendance_record ar
                    ON ar.course_session_id=? AND ar.enrollment_id=e.id
                  LEFT JOIN score_record sr
                    ON sr.course_session_id=? AND sr.enrollment_id=e.id
                 WHERE e.term_id=? AND e.teaching_group_id=? AND e.status='ACTIVE'
                 ORDER BY e.in_class_no, e.id
                """, (rs, rowNum) -> {
            AttendanceRecordVO vo = new AttendanceRecordVO();
            vo.setId(nullableLong(rs, "attendance_id"));
            vo.setScoreRecordId(nullableLong(rs, "score_record_id"));
            vo.setSessionId(sessionId);
            vo.setEnrollmentId(rs.getLong("enrollment_id"));
            vo.setStudentId(nullableLong(rs, "student_id"));
            vo.setStudentNo(rs.getString("student_no_snapshot"));
            vo.setStudentName(rs.getString("student_name_snapshot"));
            vo.setStudentNoInClass(rs.getInt("in_class_no"));
            boolean assistant = rs.getInt("is_assistant") == 1;
            vo.setIsAssistant(assistant ? 1 : 0);
            vo.setExcludedFromScoring(assistant);
            String attendanceType = rs.getString("attendance_type");
            String mark = rs.getString("score_mark");
            vo.setAttendanceType(mark == null ? attendanceType : mark);
            vo.setScore(rs.getBigDecimal("score_value"));
            vo.setScoreMark(mark);
            vo.setRemark(rs.getString("remark"));
            vo.setRecordedBy(nullableLong(rs, "recorded_by_user_id"));
            vo.setUpdatedAt(dateTime(rs, "updated_at"));
            Integer revisions = nullableInt(rs, "revision_count");
            vo.setRevisionCount(revisions == null ? 0 : revisions);
            Long currentRequestId = nullableLong(rs, "current_request_id");
            String requestStatus = rs.getString("current_request_status");
            Long approvedRequestId = "APPROVED".equals(requestStatus) ? currentRequestId : null;
            boolean hasScore = vo.getScoreRecordId() != null;
            boolean approvalRequired = hasScore && (!window.editable()
                    || vo.getRevisionCount() >= 1 || rs.getBoolean("locked"));
            vo.setRequiresApproval(approvalRequired);
            vo.setAvailableChangeRequestId(approvedRequestId);
            vo.setApprovalStatus("PENDING".equals(requestStatus) ? "pending"
                    : ("APPROVED".equals(requestStatus) ? "approved" : ""));
            vo.setEditable(!assistant && (approvedRequestId != null
                    || (window.editable() && (!hasScore || vo.getRevisionCount() < 1))));
            return vo;
        }, user.id(), user.id(), sessionId, sessionId, session.termId(), session.groupId());
    }

    @Transactional(rollbackFor = Exception.class)
    public void saveRecord(Long sessionId, Long studentId, AttendanceRecordSaveDTO dto) {
        CurrentUser user = currentUser();
        SessionContext session = requireSessionForUpdate(sessionId);
        checkGroupAccess(session.groupId());
        if ("ARCHIVED".equals(session.status())) {
            throw new BusinessException(ResultCode.DATA_SEALED, "课次已归档，不能修改");
        }
        EnrollmentContext enrollment = requireEnrollment(session, studentId);
        preventAssistantTarget(enrollment.studentId());

        ScoreInput input = validateScoreInput(dto, session.maxScore());
        ScoreContext existing = findScoreForUpdate(sessionId, enrollment.id());
        boolean creating = existing == null;
        SessionWindow window = evaluateWindow(session.sessionDate(), session.deadline(), session.status());

        Long requestId = null;
        ChangeRequestContext approvedRequest = null;
        if (creating) {
            if (!window.editable() && !user.teacherOrAdmin()) {
                throw new BusinessException(ResultCode.DATA_SEALED,
                        "首次超期补录须由教师或管理员在Web端完成");
            }
            if (input.numeric() && input.value().compareTo(new BigDecimal("5")) != 0
                    && !StringUtils.hasText(dto.getRemark())) {
                throw new BusinessException("首次录入非5分时必须填写备注");
            }
        } else {
            boolean kToJ = "K".equals(existing.mark()) && "J".equals(input.mark());
            boolean approvalRequired = !window.editable() || existing.revisionCount() >= 1
                    || existing.locked() || kToJ;
            if (approvalRequired) {
                approvedRequest = requireUsableApproval(existing.id(), dto.getChangeRequestId(), user.id());
                requestId = approvedRequest.id();
            }
        }

        Long attendanceId = upsertAttendance(sessionId, enrollment.id(), input.attendanceType(), user);
        if (creating) {
            insertScore(sessionId, enrollment.id(), attendanceId, input, dto.getRemark(), user);
            return;
        }

        int nextRevision = existing.revisionCount() + 1;
        String reason = firstText(dto.getChangeReason(), dto.getRemark(),
                approvedRequest == null ? null : approvedRequest.reason(), "课次成绩修改");
        jdbc.update("""
                INSERT INTO score_revision
                    (score_record_id, revision_no, old_score_value, old_score_mark,
                     new_score_value, new_score_mark, change_reason, change_request_id,
                     changed_by_user_id, changed_by_name_snapshot)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, existing.id(), nextRevision, existing.value(), existing.mark(),
                input.value(), input.mark(), reason, requestId, user.id(), user.realName());
        jdbc.update("""
                UPDATE score_record
                   SET attendance_record_id=?, score_value=?, score_mark=?, remark=?,
                       revision_count=?, locked=?, last_recorded_by_user_id=?,
                       last_recorded_by_name_snapshot=?
                 WHERE id=?
                """, attendanceId, input.value(), input.mark(), dto.getRemark(), nextRevision,
                approvedRequest != null, user.id(), user.realName(), existing.id());
        if (approvedRequest != null) {
            consumeApproval(approvedRequest, user);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public int batchScore(Long sessionId, AttendanceBatchScoreDTO dto) {
        int success = 0;
        for (Long studentId : dto.getStudentIds()) {
            if (isAssistantStudent(studentId) || hasScoreForStudent(sessionId, studentId)) {
                continue;
            }
            AttendanceRecordSaveDTO single = new AttendanceRecordSaveDTO();
            single.setAttendanceType(dto.getAttendanceType() == null ? "NORMAL" : dto.getAttendanceType());
            single.setScore(dto.getScore());
            single.setRemark(dto.getRemark());
            saveRecord(sessionId, studentId, single);
            success++;
        }
        return success;
    }

    @Transactional(rollbackFor = Exception.class)
    public Long submitModificationRequest(ModificationRequestCreateDTO dto) {
        CurrentUser user = currentUser();
        SessionContext session = requireSession(dto.getSessionId());
        checkGroupAccess(session.groupId());
        EnrollmentContext enrollment = requireEnrollment(session, dto.getStudentId());
        preventAssistantTarget(enrollment.studentId());
        ScoreContext score = findScore(session.id(), enrollment.id());
        if (score == null) {
            throw new BusinessException("该学生尚无成绩，不能提交修改申请；首次超期补录由教师或管理员处理");
        }
        expireApprovalIfNeeded(score.id());
        Long activeId = queryLong("""
                SELECT id FROM change_request
                 WHERE target_table='score_record' AND target_id=? AND applicant_user_id=?
                   AND status IN ('PENDING','APPROVED')
                 ORDER BY created_at DESC LIMIT 1
                """, score.id(), user.id());
        if (activeId != null) {
            throw new BusinessException("该成绩已有待审批或待使用的修改申请，申请ID=" + activeId);
        }
        String requestNo = "CR" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase(Locale.ROOT);
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            var ps = connection.prepareStatement("""
                    INSERT INTO change_request
                        (request_no, request_type, target_table, target_id,
                         applicant_user_id, applicant_name_snapshot, reason, status)
                    VALUES (?, 'SCORE_EDIT', 'score_record', ?, ?, ?, ?, 'PENDING')
                    """, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, requestNo);
            ps.setLong(2, score.id());
            ps.setLong(3, user.id());
            ps.setString(4, user.realName());
            ps.setString(5, dto.getReason().trim());
            return ps;
        }, keyHolder);
        Long requestId = keyHolder.getKey().longValue();
        insertAction(requestId, "SUBMIT", user.id(), user.realName(), dto.getReason().trim());
        return requestId;
    }

    @Transactional(rollbackFor = Exception.class)
    public List<ChangeRequestVO> listModificationRequests(String status, Long sessionId) {
        expireTimedOutApprovals();
        CurrentUser user = currentUser();
        if (StringUtils.hasText(status) && !REQUEST_STATUSES.contains(status.toUpperCase(Locale.ROOT))) {
            throw new BusinessException("申请状态不合法");
        }
        StringBuilder sql = new StringBuilder(changeRequestSelectSql());
        List<Object> args = new ArrayList<>();
        sql.append(" WHERE cr.target_table='score_record' ");
        if (user.admin()) {
            // 管理员查看全部。
        } else if (user.teacher()) {
            sql.append(" AND tg.current_teacher_user_id=? ");
            args.add(user.id());
        } else {
            sql.append(" AND cr.applicant_user_id=? ");
            args.add(user.id());
        }
        if (StringUtils.hasText(status)) {
            sql.append(" AND cr.status=? ");
            args.add(status.toUpperCase(Locale.ROOT));
        }
        if (sessionId != null) {
            sql.append(" AND cs.id=? ");
            args.add(sessionId);
        }
        sql.append(" ORDER BY cr.created_at DESC ");
        return jdbc.query(sql.toString(), (rs, rowNum) -> mapChangeRequest(rs), args.toArray());
    }

    @Transactional(rollbackFor = Exception.class)
    public ChangeRequestVO getModificationRequest(Long requestId) {
        expireTimedOutApprovals();
        CurrentUser user = currentUser();
        ChangeRequestVO vo = queryChangeRequest(requestId);
        checkRequestReadAccess(vo, user);
        vo.setActions(listActions(requestId));
        return vo;
    }

    @Transactional(rollbackFor = Exception.class)
    public void decideModificationRequest(Long requestId, ApprovalDecisionDTO dto) {
        CurrentUser reviewer = currentUser();
        ReviewContext request = requireReviewContextForUpdate(requestId);
        checkReviewerAccess(request.groupId(), reviewer);
        if (java.util.Objects.equals(request.applicantUserId(), reviewer.id())) {
            throw new BusinessException(ResultCode.PERMISSION_DENIED, "申请人不能审批自己的申请");
        }
        if (!"PENDING".equals(request.status())) {
            throw new BusinessException("只有待审批申请可以处理，当前状态=" + request.status());
        }
        String decision = dto.getDecision().toUpperCase(Locale.ROOT);
        if ("APPROVE".equals(decision)) {
            int hours = dto.getEditWindowHours() == null
                    ? readIntConfig("score.approved_edit_window_hours", 24)
                    : dto.getEditWindowHours();
            LocalDateTime now = LocalDateTime.now();
            jdbc.update("""
                    UPDATE change_request
                       SET status='APPROVED', approved_at=?, edit_window_expires_at=?
                     WHERE id=?
                    """, now, now.plusHours(hours), requestId);
            insertAction(requestId, "APPROVE", reviewer.id(), reviewer.realName(), dto.getComment());
        } else {
            jdbc.update("UPDATE change_request SET status='REJECTED' WHERE id=?", requestId);
            insertAction(requestId, "REJECT", reviewer.id(), reviewer.realName(), dto.getComment());
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void cancelModificationRequest(Long requestId) {
        CurrentUser user = currentUser();
        ChangeRequestContext request = requireChangeRequestForUpdate(requestId);
        if (!java.util.Objects.equals(request.applicantUserId(), user.id())) {
            throw new BusinessException(ResultCode.PERMISSION_DENIED, "只能取消本人提交的申请");
        }
        if (!"PENDING".equals(request.status())) {
            throw new BusinessException("只有待审批申请可以取消");
        }
        jdbc.update("UPDATE change_request SET status='CANCELLED' WHERE id=?", requestId);
        insertAction(requestId, "CANCEL", user.id(), user.realName(), "申请人取消");
    }

    public List<ApprovalActionVO> listActions(Long requestId) {
        ChangeRequestVO request = queryChangeRequest(requestId);
        checkRequestReadAccess(request, currentUser());
        return jdbc.query("""
                SELECT id, action, operator_user_id, operator_name_snapshot,
                       comment_text, action_at
                  FROM approval_action
                 WHERE change_request_id=?
                 ORDER BY action_at, id
                """, (rs, rowNum) -> {
            ApprovalActionVO vo = new ApprovalActionVO();
            vo.setId(rs.getLong("id"));
            vo.setAction(rs.getString("action"));
            vo.setOperatorUserId(nullableLong(rs, "operator_user_id"));
            vo.setOperatorName(rs.getString("operator_name_snapshot"));
            vo.setComment(rs.getString("comment_text"));
            vo.setActionAt(dateTime(rs, "action_at"));
            return vo;
        }, requestId);
    }

    private int countUnscored(Long sessionId, boolean editable) {
        if (!editable) {
            return 0;
        }
        Integer value = jdbc.queryForObject("""
                SELECT COUNT(*)
                  FROM student_course_enrollment e
                  JOIN course_session cs
                    ON cs.term_id=e.term_id AND cs.teaching_group_id=e.teaching_group_id
                  LEFT JOIN assistant_profile ap
                    ON ap.student_id=e.student_id AND ap.status='ACTIVE'
                  LEFT JOIN score_record sr
                    ON sr.course_session_id=cs.id AND sr.enrollment_id=e.id
                 WHERE cs.id=? AND e.status='ACTIVE' AND ap.id IS NULL AND sr.id IS NULL
                """, Integer.class, sessionId);
        return value == null ? 0 : value;
    }

    private SessionContext requireSession(Long sessionId) {
        try {
            return jdbc.queryForObject("""
                    SELECT id, term_id, teaching_group_id, session_date,
                           score_deadline_at, max_score, status
                      FROM course_session WHERE id=?
                    """, (rs, rowNum) -> mapSessionContext(rs), sessionId);
        } catch (EmptyResultDataAccessException ex) {
            throw new BusinessException("劳动课课次不存在");
        }
    }

    private SessionContext requireSessionForUpdate(Long sessionId) {
        try {
            return jdbc.queryForObject("""
                    SELECT id, term_id, teaching_group_id, session_date,
                           score_deadline_at, max_score, status
                      FROM course_session WHERE id=? FOR UPDATE
                    """, (rs, rowNum) -> mapSessionContext(rs), sessionId);
        } catch (EmptyResultDataAccessException ex) {
            throw new BusinessException("劳动课课次不存在");
        }
    }

    private SessionContext mapSessionContext(ResultSet rs) throws SQLException {
        return new SessionContext(rs.getLong("id"), rs.getLong("term_id"),
                nullableLong(rs, "teaching_group_id"), rs.getDate("session_date").toLocalDate(),
                dateTime(rs, "score_deadline_at"), rs.getBigDecimal("max_score"),
                rs.getString("status"));
    }

    private EnrollmentContext requireEnrollment(SessionContext session, Long studentId) {
        try {
            return jdbc.queryForObject("""
                    SELECT id, student_id, student_no_snapshot, student_name_snapshot
                      FROM student_course_enrollment
                     WHERE term_id=? AND teaching_group_id=? AND student_id=? AND status='ACTIVE'
                     ORDER BY attempt_no DESC LIMIT 1
                    """, (rs, rowNum) -> new EnrollmentContext(rs.getLong("id"),
                    nullableLong(rs, "student_id"), rs.getString("student_no_snapshot"),
                    rs.getString("student_name_snapshot")),
                    session.termId(), session.groupId(), studentId);
        } catch (EmptyResultDataAccessException ex) {
            throw new BusinessException("该学生不属于此课次对应教学分组");
        }
    }

    private ScoreContext findScore(Long sessionId, Long enrollmentId) {
        try {
            return jdbc.queryForObject("""
                    SELECT id, score_value, score_mark, revision_count, locked
                      FROM score_record
                     WHERE course_session_id=? AND enrollment_id=?
                    """, (rs, rowNum) -> mapScoreContext(rs), sessionId, enrollmentId);
        } catch (EmptyResultDataAccessException ex) {
            return null;
        }
    }

    private ScoreContext findScoreForUpdate(Long sessionId, Long enrollmentId) {
        try {
            return jdbc.queryForObject("""
                    SELECT id, score_value, score_mark, revision_count, locked
                      FROM score_record
                     WHERE course_session_id=? AND enrollment_id=? FOR UPDATE
                    """, (rs, rowNum) -> mapScoreContext(rs), sessionId, enrollmentId);
        } catch (EmptyResultDataAccessException ex) {
            return null;
        }
    }

    private ScoreContext mapScoreContext(ResultSet rs) throws SQLException {
        return new ScoreContext(rs.getLong("id"), rs.getBigDecimal("score_value"),
                rs.getString("score_mark"), rs.getInt("revision_count"), rs.getBoolean("locked"));
    }

    private ScoreInput validateScoreInput(AttendanceRecordSaveDTO dto, BigDecimal maxScore) {
        String type = dto.getAttendanceType().trim().toUpperCase(Locale.ROOT);
        if (!Set.of("NORMAL", "J", "K").contains(type)) {
            throw new BusinessException("考勤类型只能是 NORMAL、J 或 K");
        }
        if ("NORMAL".equals(type)) {
            if (dto.getScore() == null) {
                throw new BusinessException("正常出勤时分数不能为空");
            }
            if (dto.getScore().compareTo(BigDecimal.ZERO) < 0 || dto.getScore().compareTo(maxScore) > 0) {
                throw new BusinessException("分数必须在0到" + maxScore.stripTrailingZeros().toPlainString() + "之间");
            }
            return new ScoreInput("NORMAL", dto.getScore(), null, true);
        }
        return new ScoreInput("J".equals(type) ? "LEAVE" : "ABSENCE", null, type, false);
    }

    private Long upsertAttendance(Long sessionId, Long enrollmentId, String type, CurrentUser user) {
        Long id = queryLong("""
                SELECT id FROM attendance_record
                 WHERE course_session_id=? AND enrollment_id=? FOR UPDATE
                """, sessionId, enrollmentId);
        if (id != null) {
            jdbc.update("""
                    UPDATE attendance_record
                       SET attendance_type=?, recorded_by_user_id=?,
                           recorded_by_name_snapshot=?, version_no=version_no+1
                     WHERE id=?
                    """, type, user.id(), user.realName(), id);
            return id;
        }
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            var ps = connection.prepareStatement("""
                    INSERT INTO attendance_record
                        (course_session_id, enrollment_id, attendance_type,
                         recorded_by_user_id, recorded_by_name_snapshot)
                    VALUES (?, ?, ?, ?, ?)
                    """, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, sessionId);
            ps.setLong(2, enrollmentId);
            ps.setString(3, type);
            ps.setLong(4, user.id());
            ps.setString(5, user.realName());
            return ps;
        }, keyHolder);
        return keyHolder.getKey().longValue();
    }

    private void insertScore(Long sessionId, Long enrollmentId, Long attendanceId,
                             ScoreInput input, String remark, CurrentUser user) {
        jdbc.update("""
                INSERT INTO score_record
                    (course_session_id, enrollment_id, attendance_record_id,
                     score_value, score_mark, remark, revision_count, locked,
                     last_recorded_by_user_id, last_recorded_by_name_snapshot)
                VALUES (?, ?, ?, ?, ?, ?, 0, FALSE, ?, ?)
                """, sessionId, enrollmentId, attendanceId, input.value(), input.mark(),
                remark, user.id(), user.realName());
    }

    private ChangeRequestContext requireUsableApproval(Long scoreRecordId, Long requestedId, Long userId) {
        ChangeRequestContext request;
        try {
            if (requestedId != null) {
                request = jdbc.queryForObject("""
                        SELECT id, applicant_user_id, status, reason, edit_window_expires_at
                          FROM change_request
                         WHERE id=? AND target_table='score_record' AND target_id=? FOR UPDATE
                        """, (rs, rowNum) -> mapChangeRequestContext(rs), requestedId, scoreRecordId);
            } else {
                request = jdbc.queryForObject("""
                        SELECT id, applicant_user_id, status, reason, edit_window_expires_at
                          FROM change_request
                         WHERE target_table='score_record' AND target_id=?
                           AND applicant_user_id=? AND status='APPROVED'
                         ORDER BY approved_at DESC LIMIT 1 FOR UPDATE
                        """, (rs, rowNum) -> mapChangeRequestContext(rs), scoreRecordId, userId);
            }
        } catch (EmptyResultDataAccessException ex) {
            throw new BusinessException("本次修改需要先提交申请并由教师审批通过");
        }
        if (!java.util.Objects.equals(request.applicantUserId(), userId)) {
            throw new BusinessException(ResultCode.PERMISSION_DENIED, "该审批不属于当前用户");
        }
        if (!"APPROVED".equals(request.status())) {
            throw new BusinessException("修改申请尚未通过或已被使用");
        }
        if (request.expiresAt() == null || request.expiresAt().isBefore(LocalDateTime.now())) {
            jdbc.update("UPDATE change_request SET status='EXPIRED' WHERE id=?", request.id());
            insertAction(request.id(), "EXPIRE", null, "系统", "审批修改窗口已过期");
            throw new BusinessException("审批修改窗口已过期，请重新提交申请");
        }
        return request;
    }

    private ChangeRequestContext requireChangeRequestForUpdate(Long requestId) {
        try {
            return jdbc.queryForObject("""
                    SELECT id, applicant_user_id, status, reason, edit_window_expires_at
                      FROM change_request WHERE id=? FOR UPDATE
                    """, (rs, rowNum) -> mapChangeRequestContext(rs), requestId);
        } catch (EmptyResultDataAccessException ex) {
            throw new BusinessException("修改申请不存在");
        }
    }

    private ChangeRequestContext mapChangeRequestContext(ResultSet rs) throws SQLException {
        return new ChangeRequestContext(rs.getLong("id"), nullableLong(rs, "applicant_user_id"),
                rs.getString("status"), rs.getString("reason"),
                dateTime(rs, "edit_window_expires_at"));
    }

    private ReviewContext requireReviewContextForUpdate(Long requestId) {
        try {
            return jdbc.queryForObject("""
                    SELECT cr.id, cr.status, cr.applicant_user_id, cs.teaching_group_id
                      FROM change_request cr
                      JOIN score_record sr ON cr.target_table='score_record' AND cr.target_id=sr.id
                      JOIN course_session cs ON cs.id=sr.course_session_id
                     WHERE cr.id=? FOR UPDATE
                    """, (rs, rowNum) -> new ReviewContext(rs.getLong("id"),
                    rs.getString("status"), nullableLong(rs, "applicant_user_id"),
                    rs.getLong("teaching_group_id")), requestId);
        } catch (EmptyResultDataAccessException ex) {
            throw new BusinessException("修改申请不存在");
        }
    }

    private void consumeApproval(ChangeRequestContext request, CurrentUser user) {
        LocalDateTime now = LocalDateTime.now();
        int updated = jdbc.update("""
                UPDATE change_request SET status='USED', used_at=?
                 WHERE id=? AND status='APPROVED' AND edit_window_expires_at>=?
                """, now, request.id(), now);
        if (updated != 1) {
            throw new BusinessException("审批已被使用或已过期");
        }
        insertAction(request.id(), "USE", user.id(), user.realName(), "已完成成绩修改并重新锁定");
    }

    private void expireApprovalIfNeeded(Long scoreRecordId) {
        List<Long> expiredIds = jdbc.queryForList("""
                SELECT id FROM change_request
                 WHERE target_table='score_record' AND target_id=? AND status='APPROVED'
                   AND edit_window_expires_at<CURRENT_TIMESTAMP(3)
                """, Long.class, scoreRecordId);
        for (Long id : expiredIds) {
            if (jdbc.update("UPDATE change_request SET status='EXPIRED' WHERE id=? AND status='APPROVED'", id) == 1) {
                insertAction(id, "EXPIRE", null, "系统", "审批修改窗口已过期");
            }
        }
    }

    private void expireTimedOutApprovals() {
        List<Long> expiredIds = jdbc.queryForList("""
                SELECT id FROM change_request
                 WHERE status='APPROVED'
                   AND edit_window_expires_at<CURRENT_TIMESTAMP(3)
                """, Long.class);
        for (Long id : expiredIds) {
            if (jdbc.update("UPDATE change_request SET status='EXPIRED' WHERE id=? AND status='APPROVED'", id) == 1) {
                insertAction(id, "EXPIRE", null, "系统", "审批修改窗口已过期");
            }
        }
    }

    private void insertAction(Long requestId, String action, Long operatorId,
                              String operatorName, String comment) {
        jdbc.update("""
                INSERT INTO approval_action
                    (change_request_id, action, operator_user_id,
                     operator_name_snapshot, comment_text)
                VALUES (?, ?, ?, ?, ?)
                """, requestId, action, operatorId, operatorName, comment);
    }

    private String changeRequestSelectSql() {
        return """
                SELECT cr.id, cr.request_no, cr.request_type, cr.status, cr.reason,
                       cr.applicant_user_id, cr.applicant_name_snapshot,
                       cr.target_id AS score_record_id, cr.approved_at,
                       cr.edit_window_expires_at, cr.used_at, cr.created_at, cr.updated_at,
                       sr.score_value, sr.score_mark, cs.id AS session_id, cs.session_date,
                       tg.id AS teaching_group_id, tg.company_name_snapshot, tg.class_code,
                       e.student_id, e.student_no_snapshot, e.student_name_snapshot
                  FROM change_request cr
                  JOIN score_record sr ON cr.target_table='score_record' AND cr.target_id=sr.id
                  JOIN course_session cs ON cs.id=sr.course_session_id
                  JOIN teaching_group tg ON tg.id=cs.teaching_group_id
                  JOIN student_course_enrollment e ON e.id=sr.enrollment_id
                """;
    }

    private ChangeRequestVO queryChangeRequest(Long requestId) {
        try {
            return jdbc.queryForObject(changeRequestSelectSql() + " WHERE cr.id=?",
                    (rs, rowNum) -> mapChangeRequest(rs), requestId);
        } catch (EmptyResultDataAccessException ex) {
            throw new BusinessException("修改申请不存在");
        }
    }

    private ChangeRequestVO mapChangeRequest(ResultSet rs) throws SQLException {
        ChangeRequestVO vo = new ChangeRequestVO();
        vo.setId(rs.getLong("id"));
        vo.setRequestNo(rs.getString("request_no"));
        vo.setRequestType(rs.getString("request_type"));
        vo.setStatus(rs.getString("status"));
        vo.setReason(rs.getString("reason"));
        vo.setApplicantUserId(nullableLong(rs, "applicant_user_id"));
        vo.setApplicantName(rs.getString("applicant_name_snapshot"));
        vo.setScoreRecordId(rs.getLong("score_record_id"));
        vo.setSessionId(rs.getLong("session_id"));
        vo.setSessionDate(rs.getDate("session_date").toLocalDate());
        vo.setTeachingGroupId(rs.getLong("teaching_group_id"));
        vo.setCompanyName(rs.getString("company_name_snapshot"));
        vo.setClassCode(rs.getString("class_code"));
        vo.setStudentId(nullableLong(rs, "student_id"));
        vo.setStudentNo(rs.getString("student_no_snapshot"));
        vo.setStudentName(rs.getString("student_name_snapshot"));
        vo.setCurrentScore(rs.getBigDecimal("score_value"));
        vo.setCurrentScoreMark(rs.getString("score_mark"));
        vo.setApprovedAt(dateTime(rs, "approved_at"));
        vo.setEditWindowExpiresAt(dateTime(rs, "edit_window_expires_at"));
        vo.setUsedAt(dateTime(rs, "used_at"));
        vo.setCreatedAt(dateTime(rs, "created_at"));
        vo.setUpdatedAt(dateTime(rs, "updated_at"));
        return vo;
    }

    private void checkRequestReadAccess(ChangeRequestVO request, CurrentUser user) {
        if (user.admin() || java.util.Objects.equals(request.getApplicantUserId(), user.id())) {
            return;
        }
        if (user.teacher()) {
            checkReviewerAccess(request.getTeachingGroupId(), user);
            return;
        }
        throw new BusinessException(ResultCode.PERMISSION_DENIED);
    }

    private void checkGroupAccess(Long groupId) {
        if (groupId == null) {
            throw new BusinessException("教学分组不存在");
        }
        CurrentUser user = currentUser();
        if (user.admin()) {
            return;
        }
        Integer count;
        if (user.teacher()) {
            count = jdbc.queryForObject("""
                    SELECT COUNT(*) FROM teaching_group
                     WHERE id=? AND current_teacher_user_id=?
                    """, Integer.class, groupId, user.id());
        } else {
            count = jdbc.queryForObject("""
                    SELECT COUNT(*)
                      FROM assistant_profile ap
                      JOIN assistant_group_assignment aga
                        ON aga.assistant_profile_id=ap.id AND aga.unassigned_at IS NULL
                     WHERE ap.user_id=? AND ap.status='ACTIVE' AND aga.teaching_group_id=?
                    """, Integer.class, user.id(), groupId);
        }
        if (count == null || count == 0) {
            throw new BusinessException(ResultCode.PERMISSION_DENIED, "无权访问该教学分组");
        }
    }

    private void checkReviewerAccess(Long groupId, CurrentUser user) {
        if (user.admin()) {
            return;
        }
        if (!user.teacher()) {
            throw new BusinessException(ResultCode.PERMISSION_DENIED, "只有任课教师或管理员可以审批");
        }
        Integer count = jdbc.queryForObject("""
                SELECT COUNT(*) FROM teaching_group
                 WHERE id=? AND current_teacher_user_id=?
                """, Integer.class, groupId, user.id());
        if (count == null || count == 0) {
            throw new BusinessException(ResultCode.PERMISSION_DENIED, "只能审批本人教学分组的申请");
        }
    }

    private void preventAssistantTarget(Long studentId) {
        if (studentId != null && isAssistantStudent(studentId)) {
            throw new BusinessException(ResultCode.PERMISSION_DENIED, "助教身份学生不参与考勤打分");
        }
    }

    private boolean isAssistantStudent(Long studentId) {
        Integer count = jdbc.queryForObject("""
                SELECT COUNT(*) FROM assistant_profile
                 WHERE student_id=? AND status='ACTIVE'
                """, Integer.class, studentId);
        return count != null && count > 0;
    }

    private boolean hasScoreForStudent(Long sessionId, Long studentId) {
        Integer count = jdbc.queryForObject("""
                SELECT COUNT(*)
                  FROM score_record sr
                  JOIN student_course_enrollment e ON e.id=sr.enrollment_id
                 WHERE sr.course_session_id=? AND e.student_id=?
                """, Integer.class, sessionId, studentId);
        return count != null && count > 0;
    }

    private SessionWindow evaluateWindow(LocalDate sessionDate, LocalDateTime deadline, String status) {
        LocalDateTime now = LocalDateTime.now();
        if ("ARCHIVED".equals(status)) {
            return new SessionWindow(false, "课次已归档");
        }
        if (sessionDate != null && now.toLocalDate().isBefore(sessionDate)) {
            return new SessionWindow(false, "未到该周次上课日期");
        }
        if (!"OPEN".equals(status)) {
            return new SessionWindow(false, "课次已封存");
        }
        if (deadline != null && now.isAfter(deadline)) {
            return new SessionWindow(false, "已超过成绩修改截止时间");
        }
        return new SessionWindow(true, "可编辑");
    }

    private int readIntConfig(String key, int defaultValue) {
        try {
            String value = jdbc.queryForObject(
                    "SELECT config_value FROM business_config WHERE config_key=?",
                    String.class, key);
            return value == null ? defaultValue : Integer.parseInt(value.trim());
        } catch (RuntimeException ex) {
            return defaultValue;
        }
    }

    private CurrentUser currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof LoginUser loginUser)) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        boolean admin = hasAnyRole(auth, "ADMIN", "SUPER_ADMIN");
        boolean teacher = hasAnyRole(auth, "TEACHER");
        boolean assistant = hasAnyRole(auth, "ASSISTANT");
        return new CurrentUser(loginUser.getId(), loginUser.getRealName(), admin, teacher, assistant);
    }

    private boolean hasAnyRole(Authentication auth, String... roles) {
        Set<String> accepted = Set.of(roles);
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(accepted::contains);
    }

    private Long queryLong(String sql, Object... args) {
        try {
            return jdbc.queryForObject(sql, Long.class, args);
        } catch (EmptyResultDataAccessException ex) {
            return null;
        }
    }

    private static Long nullableLong(ResultSet rs, String column) throws SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }

    private static Integer nullableInt(ResultSet rs, String column) throws SQLException {
        int value = rs.getInt(column);
        return rs.wasNull() ? null : value;
    }

    private static LocalDateTime dateTime(ResultSet rs, String column) throws SQLException {
        Timestamp value = rs.getTimestamp(column);
        return value == null ? null : value.toLocalDateTime();
    }

    private static String firstText(String... candidates) {
        for (String value : candidates) {
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return "成绩修改";
    }

    private record CurrentUser(Long id, String realName, boolean admin,
                               boolean teacher, boolean assistant) {
        boolean teacherOrAdmin() {
            return admin || teacher;
        }
    }

    private record SessionContext(Long id, Long termId, Long groupId, LocalDate sessionDate,
                                  LocalDateTime deadline, BigDecimal maxScore, String status) {
    }

    private record EnrollmentContext(Long id, Long studentId, String studentNo, String studentName) {
    }

    private record ScoreContext(Long id, BigDecimal value, String mark,
                                int revisionCount, boolean locked) {
    }

    private record ScoreInput(String attendanceType, BigDecimal value, String mark, boolean numeric) {
    }

    private record ChangeRequestContext(Long id, Long applicantUserId, String status,
                                        String reason, LocalDateTime expiresAt) {
    }

    private record ReviewContext(Long id, String status, Long applicantUserId, Long groupId) {
    }

    private record SessionWindow(boolean editable, String reason) {
    }
}
