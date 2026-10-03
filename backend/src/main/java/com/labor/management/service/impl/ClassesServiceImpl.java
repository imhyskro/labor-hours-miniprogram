package com.labor.management.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.labor.management.dto.ClassCreateDTO;
import com.labor.management.dto.ClassUpdateDTO;
import com.labor.management.entity.Classes;
import com.labor.management.exception.BusinessException;
import com.labor.management.service.ClassesService;
import com.labor.management.vo.ClassStudentVO;
import com.labor.management.vo.ClassVO;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/** Web-compatible class service backed by V2 teaching_group and enrollment tables. */
@Service
@RequiredArgsConstructor
public class ClassesServiceImpl implements ClassesService {
    private static final int[][] PERIODS = {{1, 2}, {3, 4}, {5, 6}, {7, 8}};
    private final JdbcTemplate jdbcTemplate;
    private final NamedParameterJdbcTemplate namedJdbcTemplate;

    @Override
    public IPage<ClassVO> pageQuery(Integer page, Integer size, List<Long> scopeClassIds) {
        int current = page == null || page < 1 ? 1 : page;
        int pageSize = size == null || size < 1 ? 10 : size;
        MapSqlParameterSource params = new MapSqlParameterSource().addValue("offset", (current - 1) * pageSize)
                .addValue("size", pageSize);
        String where = scopeClause(scopeClassIds, params);
        Long total = namedJdbcTemplate.queryForObject("SELECT COUNT(*) FROM teaching_group g WHERE g.status <> 'ARCHIVED'" + where,
                params, Long.class);
        List<ClassVO> records = namedJdbcTemplate.query(classSelect() +
                " WHERE g.status <> 'ARCHIVED'" + where +
                " ORDER BY g.company_id, g.week, g.class_code LIMIT :offset, :size", params, this::mapClass);
        Page<ClassVO> result = new Page<>(current, pageSize, total == null ? 0 : total);
        result.setRecords(records);
        return result;
    }

    @Override
    public ClassVO getById(Long id, List<Long> scopeClassIds) {
        checkScope(id, scopeClassIds);
        List<ClassVO> rows = jdbcTemplate.query(classSelect() + " WHERE g.id = ?", this::mapClass, id);
        if (rows.isEmpty()) throw new BusinessException("班级不存在");
        return rows.get(0);
    }

    @Override
    @Transactional
    public void create(ClassCreateDTO dto) {
        validate(dto.getWeek(), dto.getStartSession(), dto.getEndSession());
        CompanyRow company = company(dto.getCompanyId());
        long termId = activeTermId();
        String code = code(dto.getWeek(), dto.getStartSession(), dto.getEndSession());
        Integer exists = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM teaching_group
                WHERE term_id = ? AND company_code_snapshot = ? AND class_code = ?
                """, Integer.class, termId, company.code(), code);
        if (exists != null && exists > 0) throw new BusinessException("该公司下已存在相同周几和节次的班级（" + code + "）");
        jdbcTemplate.update("""
                INSERT INTO teaching_group(term_id, company_id, company_code_snapshot,
                    company_name_snapshot, class_code, week, status)
                VALUES (?, ?, ?, ?, ?, ?, 'ACTIVE')
                """, termId, company.id(), company.code(), company.name(), code, dto.getWeek());
    }

    @Override
    @Transactional
    public void update(ClassUpdateDTO dto, List<Long> scopeClassIds) {
        checkScope(dto.getId(), scopeClassIds);
        validate(dto.getWeek(), dto.getStartSession(), dto.getEndSession());
        List<Map<String, Object>> groups = jdbcTemplate.queryForList("""
                SELECT id, term_id, company_code_snapshot FROM teaching_group WHERE id = ?
                """, dto.getId());
        if (groups.isEmpty()) throw new BusinessException("班级不存在");
        Map<String, Object> group = groups.get(0);
        String newCode = code(dto.getWeek(), dto.getStartSession(), dto.getEndSession());
        Integer duplicate = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM teaching_group
                WHERE term_id = ? AND company_code_snapshot = ? AND class_code = ? AND id <> ?
                """, Integer.class, group.get("term_id"), group.get("company_code_snapshot"), newCode, dto.getId());
        if (duplicate != null && duplicate > 0) throw new BusinessException("该公司下已存在相同周几和节次的班级（" + newCode + "）");
        String status = dto.getStatus() != null && dto.getStatus() == 0 ? "ARCHIVED" : "ACTIVE";
        jdbcTemplate.update("UPDATE teaching_group SET class_code = ?, week = ?, status = ? WHERE id = ?",
                newCode, dto.getWeek(), status, dto.getId());
        jdbcTemplate.update("""
                UPDATE student_course_enrollment SET class_code_snapshot = ?
                WHERE teaching_group_id = ? AND status = 'ACTIVE'
                """, newCode, dto.getId());
    }

    @Override
    @Transactional
    public void deleteById(Long id, List<Long> scopeClassIds) {
        checkScope(id, scopeClassIds);
        Integer groupCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM teaching_group WHERE id = ?", Integer.class, id);
        if (groupCount == null || groupCount == 0) throw new BusinessException("班级不存在");
        Integer facts = jdbcTemplate.queryForObject("""
                SELECT (SELECT COUNT(*) FROM student_course_enrollment WHERE teaching_group_id = ?)
                     + (SELECT COUNT(*) FROM course_session WHERE teaching_group_id = ?)
                """, Integer.class, id, id);
        if (facts != null && facts > 0) throw new BusinessException("该班级已有学生或课次，不能删除，可改为归档");
        jdbcTemplate.update("DELETE FROM teaching_group WHERE id = ?", id);
    }

    @Override
    public List<ClassVO> listAll(List<Long> scopeClassIds) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        String where = scopeClause(scopeClassIds, params);
        return namedJdbcTemplate.query(classSelect() + " WHERE g.status <> 'ARCHIVED'" + where +
                " ORDER BY g.company_id, g.week, g.class_code", params, this::mapClass);
    }

    @Override
    public List<ClassVO> listByCompany(Long companyId, List<Long> scopeClassIds) {
        company(companyId);
        MapSqlParameterSource params = new MapSqlParameterSource().addValue("companyId", companyId);
        String where = scopeClause(scopeClassIds, params);
        return namedJdbcTemplate.query(classSelect() + " WHERE g.company_id = :companyId AND g.status <> 'ARCHIVED'" + where +
                " ORDER BY g.week, g.class_code", params, this::mapClass);
    }

    @Override
    public List<ClassStudentVO> getClassStudents(Long classId, List<Long> scopeClassIds) {
        checkScope(classId, scopeClassIds);
        return jdbcTemplate.query("""
                SELECT s.id, e.student_no_snapshot, e.student_name_snapshot, e.in_class_no,
                       e.section_code, e.admin_class_snapshot, e.gender_snapshot,
                       EXISTS(SELECT 1 FROM assistant_profile ap
                              WHERE ap.student_id = s.id AND ap.status = 'ACTIVE') is_assistant
                FROM student_course_enrollment e
                LEFT JOIN student s ON s.id = e.student_id
                WHERE e.teaching_group_id = ? AND e.status <> 'WITHDRAWN'
                ORDER BY e.in_class_no
                """, (rs, n) -> {
            ClassStudentVO vo = new ClassStudentVO();
            vo.setId(rs.getLong("id")); vo.setStudentId(rs.getString("student_no_snapshot"));
            vo.setName(rs.getString("student_name_snapshot")); vo.setStudentNoInClass(rs.getInt("in_class_no"));
            vo.setFullNo(rs.getString("section_code")); vo.setOriginalMajor(rs.getString("admin_class_snapshot"));
            vo.setGender(genderNumber(rs.getString("gender_snapshot"))); vo.setIsAssistant(rs.getInt("is_assistant"));
            return vo;
        }, classId);
    }

    @Override
    @Transactional
    public Classes findOrCreateClass(Long companyId, Integer week, Integer startSession, Integer endSession) {
        String classCode = code(week, startSession, endSession);
        List<Long> ids = jdbcTemplate.queryForList("""
                SELECT g.id FROM teaching_group g JOIN academic_term t ON t.id = g.term_id
                WHERE t.status = 'ACTIVE' AND g.company_id = ? AND g.class_code = ?
                """, Long.class, companyId, classCode);
        if (ids.isEmpty()) {
            ClassCreateDTO dto = new ClassCreateDTO(); dto.setCompanyId(companyId); dto.setWeek(week);
            dto.setStartSession(startSession); dto.setEndSession(endSession); create(dto);
            ids = jdbcTemplate.queryForList("""
                    SELECT g.id FROM teaching_group g JOIN academic_term t ON t.id = g.term_id
                    WHERE t.status = 'ACTIVE' AND g.company_id = ? AND g.class_code = ?
                    """, Long.class, companyId, classCode);
        }
        return asLegacyEntity(ids.get(0));
    }

    @Override
    @Transactional
    public int ensureDefaultClasses(Long companyId) {
        int created = 0;
        for (int week = 1; week <= 5; week++) {
            for (int[] period : PERIODS) {
                String classCode = code(week, period[0], period[1]);
                Integer count = jdbcTemplate.queryForObject("""
                        SELECT COUNT(*) FROM teaching_group g JOIN academic_term t ON t.id = g.term_id
                        WHERE t.status = 'ACTIVE' AND g.company_id = ? AND g.class_code = ?
                        """, Integer.class, companyId, classCode);
                if (count == null || count == 0) {
                    ClassCreateDTO dto = new ClassCreateDTO(); dto.setCompanyId(companyId); dto.setWeek(week);
                    dto.setStartSession(period[0]); dto.setEndSession(period[1]); create(dto); created++;
                }
            }
        }
        return created;
    }

    private String classSelect() {
        return """
                SELECT g.id, CONCAT(g.company_name_snapshot, '-周', g.week, '-', g.class_code, '节') class_name,
                       g.class_code, g.company_id, g.company_name_snapshot company_name, g.week,
                       CAST(SUBSTRING_INDEX(SUBSTRING_INDEX(g.class_code, '-', 2), '-', -1) AS UNSIGNED) start_session,
                       CAST(SUBSTRING_INDEX(g.class_code, '-', -1) AS UNSIGNED) end_session,
                       t.academic_year, CASE WHEN g.status='ARCHIVED' THEN 0 ELSE 1 END web_status,
                       g.created_at,
                       (SELECT COUNT(*) FROM student_course_enrollment e WHERE e.teaching_group_id=g.id AND e.status<>'WITHDRAWN') student_count,
                       CASE WHEN g.current_teacher_user_id IS NULL THEN 0 ELSE 1 END teacher_count,
                       (SELECT COUNT(*) FROM assistant_group_assignment a WHERE a.teaching_group_id=g.id AND a.unassigned_at IS NULL) assistant_count
                FROM teaching_group g JOIN academic_term t ON t.id=g.term_id
                """;
    }

    private ClassVO mapClass(ResultSet rs, int rowNum) throws SQLException {
        ClassVO vo = new ClassVO();
        vo.setId(rs.getLong("id")); vo.setClassName(rs.getString("class_name")); vo.setClassCode(rs.getString("class_code"));
        vo.setCompanyId((Long) rs.getObject("company_id")); vo.setCompanyName(rs.getString("company_name"));
        vo.setWeek((Integer) rs.getObject("week")); vo.setStartSession(rs.getInt("start_session")); vo.setEndSession(rs.getInt("end_session"));
        vo.setAcademicYear(rs.getString("academic_year")); vo.setStatus(rs.getInt("web_status"));
        vo.setStudentCount(rs.getLong("student_count")); vo.setTeacherCount(rs.getLong("teacher_count"));
        vo.setAssistantCount(rs.getLong("assistant_count"));
        if (rs.getTimestamp("created_at") != null) vo.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        return vo;
    }

    private Classes asLegacyEntity(Long id) {
        ClassVO vo = getById(id, null);
        Classes e = new Classes(); e.setId(vo.getId()); e.setClassName(vo.getClassName()); e.setClassCode(vo.getClassCode());
        e.setCompanyId(vo.getCompanyId()); e.setWeek(vo.getWeek()); e.setStartSession(vo.getStartSession());
        e.setEndSession(vo.getEndSession()); e.setAcademicYear(vo.getAcademicYear()); e.setStatus(vo.getStatus());
        return e;
    }

    private void checkScope(Long id, List<Long> scope) {
        if (scope != null && !scope.contains(id)) throw new BusinessException("无权访问该班级");
    }
    private String scopeClause(List<Long> scope, MapSqlParameterSource params) {
        if (scope == null) return "";
        if (scope.isEmpty()) return " AND 1=0";
        params.addValue("scopeIds", scope); return " AND g.id IN (:scopeIds)";
    }
    private long activeTermId() {
        List<Long> ids = jdbcTemplate.queryForList("SELECT id FROM academic_term WHERE status='ACTIVE' ORDER BY id DESC LIMIT 1", Long.class);
        if (ids.isEmpty()) throw new BusinessException("没有启用中的学期，请先在 V2 数据库设置 ACTIVE 学期");
        return ids.get(0);
    }
    private CompanyRow company(Long id) {
        List<CompanyRow> rows = jdbcTemplate.query("SELECT id, company_code, company_name FROM company WHERE id=?",
                (rs, n) -> new CompanyRow(rs.getLong(1), rs.getString(2), rs.getString(3)), id);
        if (rows.isEmpty()) throw new BusinessException("公司不存在");
        return rows.get(0);
    }
    private void validate(Integer week, Integer start, Integer end) {
        if (week == null || week < 1 || week > 5) throw new BusinessException("周几必须在1到5之间");
        boolean valid = Arrays.stream(PERIODS).anyMatch(p -> p[0] == start && p[1] == end);
        if (!valid) throw new BusinessException("节次必须是1-2、3-4、5-6或7-8");
    }
    private String code(int week, int start, int end) { return week + "-" + start + "-" + end; }
    private int genderNumber(String value) { return "男".equals(value) ? 1 : "女".equals(value) ? 2 : 0; }
    private record CompanyRow(Long id, String code, String name) {}
}
