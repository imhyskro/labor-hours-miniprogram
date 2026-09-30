package com.labor.management.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.labor.management.common.ResultCode;
import com.labor.management.dto.AttendanceBatchScoreDTO;
import com.labor.management.dto.AttendanceRecordSaveDTO;
import com.labor.management.dto.AttendanceSessionCreateDTO;
import com.labor.management.dto.AttendanceSessionUpdateDTO;
import com.labor.management.entity.AttendanceRecord;
import com.labor.management.entity.AttendanceSession;
import com.labor.management.entity.Classes;
import com.labor.management.entity.Student;
import com.labor.management.exception.BusinessException;
import com.labor.management.mapper.AttendanceRecordMapper;
import com.labor.management.mapper.AttendanceSessionMapper;
import com.labor.management.mapper.ClassesMapper;
import com.labor.management.mapper.StudentMapper;
import com.labor.management.service.AttendanceService;
import com.labor.management.service.AttendanceSessionPolicy;
import com.labor.management.service.ClassAccessService;
import com.labor.management.service.OperationLogService;
import com.labor.management.util.SecurityUtil;
import com.labor.management.vo.AttendanceRecordVO;
import com.labor.management.vo.AttendanceSessionVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** 劳动课课次及考勤记录服务实现。 */
@Service
@RequiredArgsConstructor
public class AttendanceServiceImpl implements AttendanceService {

    private final AttendanceSessionMapper sessionMapper;
    private final AttendanceRecordMapper recordMapper;
    private final ClassesMapper classesMapper;
    private final StudentMapper studentMapper;
    private final ClassAccessService classAccessService;
    private final OperationLogService operationLogService;
    private final AttendanceSessionPolicy sessionPolicy;

    @Override
    public List<AttendanceSessionVO> listSessions(Long classId) {
        Classes classes = requireClass(classId);
        classAccessService.checkAttendanceAccess(classId);

        // 助教身份学生保留在名单中展示，但不属于可打分对象，也不计入未打分人数。
        List<Student> eligibleStudents = studentMapper.selectList(
                new LambdaQueryWrapper<Student>()
                        .eq(Student::getClassId, classId)
                        .eq(Student::getStatus, 1)
                        .and(wrapper -> wrapper.eq(Student::getIsAssistant, 0)
                                .or().isNull(Student::getIsAssistant)));
        Set<Long> eligibleStudentIds = eligibleStudents.stream()
                .map(Student::getId)
                .collect(java.util.stream.Collectors.toSet());

        // 一次拉取该班级所有课次的考勤记录，按 sessionId 分组统计已登记学生数
        List<Long> sessionIds = sessionMapper.selectList(
                        new LambdaQueryWrapper<AttendanceSession>()
                                .eq(AttendanceSession::getClassId, classId)
                                .select(AttendanceSession::getId))
                .stream().map(AttendanceSession::getId).toList();

        Map<Long, Long> scoredCountBySession = new HashMap<>();
        if (!sessionIds.isEmpty()) {
            List<AttendanceRecord> allRecords = recordMapper.selectList(
                    new LambdaQueryWrapper<AttendanceRecord>()
                            .in(AttendanceRecord::getSessionId, sessionIds));
            for (AttendanceRecord r : allRecords) {
                if (eligibleStudentIds.contains(r.getStudentId())) {
                    scoredCountBySession.merge(r.getSessionId(), 1L, Long::sum);
                }
            }
        }

        final long total = eligibleStudentIds.size();
        return sessionMapper.selectList(
                        new LambdaQueryWrapper<AttendanceSession>()
                                .eq(AttendanceSession::getClassId, classId)
                                .orderByAsc(AttendanceSession::getWeekNo))
                .stream()
                .map(entity -> {
                    AttendanceSessionVO vo = toSessionVO(entity, classes.getClassName());
                    AttendanceSessionPolicy.SessionWindow window = sessionPolicy.evaluate(entity);
                    applyWindow(vo, window);
                    long scored = scoredCountBySession.getOrDefault(entity.getId(), 0L);
                    if (!window.editable()) {
                        vo.setUnscoredCount(0);
                    } else {
                        vo.setUnscoredCount((int) Math.max(0, total - scored));
                    }
                    return vo;
                })
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createSession(AttendanceSessionCreateDTO dto) {
        requireClass(dto.getClassId());
        classAccessService.checkTeacherAccess(dto.getClassId());
        validateFlag(dto.getIsLastSession(), "是否最后一次课");
        checkSessionWeekUnique(dto.getClassId(), dto.getWeekNo(), null);

        AttendanceSession entity = new AttendanceSession();
        BeanUtils.copyProperties(dto, entity);
        entity.setIsLastSession(dto.getIsLastSession() == null ? 0 : dto.getIsLastSession());
        entity.setStatus(1);
        entity.setCreatedBy(SecurityUtil.getCurrentUserId());
        entity.setUpdatedBy(SecurityUtil.getCurrentUserId());
        sessionMapper.insert(entity);
        operationLogService.record("ATTENDANCE", "CREATE", "ATTENDANCE_SESSION", entity.getId(),
                "新建劳动课课次", null, entity);
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateSession(Long sessionId, AttendanceSessionUpdateDTO dto) {
        AttendanceSession entity = requireSession(sessionId);
        AttendanceSession before = new AttendanceSession();
        BeanUtils.copyProperties(entity, before);
        classAccessService.checkTeacherAccess(entity.getClassId());
        if (entity.getStatus() != null && entity.getStatus() == 0) {
            throw new BusinessException(ResultCode.DATA_SEALED);
        }
        validateFlag(dto.getIsLastSession(), "是否最后一次课");
        validateFlag(dto.getStatus(), "状态");
        checkSessionWeekUnique(entity.getClassId(), dto.getWeekNo(), sessionId);

        entity.setWeekNo(dto.getWeekNo());
        entity.setSessionDate(dto.getSessionDate());
        if (dto.getIsLastSession() != null) {
            entity.setIsLastSession(dto.getIsLastSession());
        }
        if (dto.getStatus() != null) {
            entity.setStatus(dto.getStatus());
        }
        entity.setUpdatedBy(SecurityUtil.getCurrentUserId());
        sessionMapper.updateById(entity);
        operationLogService.record("ATTENDANCE", "UPDATE", "ATTENDANCE_SESSION", entity.getId(),
                "修改劳动课课次", before, entity);
    }

    @Override
    public List<AttendanceRecordVO> listRecords(Long sessionId) {
        AttendanceSession session = requireSession(sessionId);
        classAccessService.checkAttendanceAccess(session.getClassId());

        List<Student> students = studentMapper.selectList(
                new LambdaQueryWrapper<Student>()
                        .eq(Student::getClassId, session.getClassId())
                        .eq(Student::getStatus, 1)
                        .orderByAsc(Student::getStudentNoInClass)
                        .orderByAsc(Student::getId));
        List<AttendanceRecord> records = recordMapper.selectList(
                new LambdaQueryWrapper<AttendanceRecord>()
                        .eq(AttendanceRecord::getSessionId, sessionId));
        Map<Long, AttendanceRecord> recordByStudent = new HashMap<>();
        for (AttendanceRecord record : records) {
            recordByStudent.put(record.getStudentId(), record);
        }
        return students.stream()
                .map(student -> toRecordVO(sessionId, student, recordByStudent.get(student.getId())))
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveRecord(Long sessionId, Long studentId, AttendanceRecordSaveDTO dto) {
        AttendanceSession session = requireEditableSession(sessionId);
        classAccessService.checkAttendanceAccess(session.getClassId());
        Student student = requireStudentInClass(studentId, session.getClassId());
        preventAssistantScoring(student);

        String type = dto.getAttendanceType().trim().toUpperCase(Locale.ROOT);
        if (!List.of("NORMAL", "J", "K").contains(type)) {
            throw new BusinessException("考勤类型只能是 NORMAL、J 或 K");
        }
        AttendanceRecord entity = recordMapper.selectOne(
                new LambdaQueryWrapper<AttendanceRecord>()
                        .eq(AttendanceRecord::getSessionId, sessionId)
                        .eq(AttendanceRecord::getStudentId, studentId));
        boolean creating = entity == null;
        AttendanceRecord before = null;
        if (creating) {
            entity = new AttendanceRecord();
            entity.setSessionId(sessionId);
            entity.setStudentId(studentId);
        } else {
            before = new AttendanceRecord();
            BeanUtils.copyProperties(entity, before);
        }
        entity.setAttendanceType(type);
        entity.setScore(dto.getScore());
        entity.setRemark(dto.getRemark());
        entity.setRecordedBy(SecurityUtil.getCurrentUserId());
        if (entity.getId() == null) {
            recordMapper.insert(entity);
        } else {
            recordMapper.updateById(entity);
        }
        operationLogService.record("ATTENDANCE", creating ? "CREATE" : "UPDATE",
                "ATTENDANCE_RECORD", entity.getId(),
                (creating ? "新增" : "修改") + "学生考勤与单次成绩", before, entity);
    }



    @Override
    @Transactional(rollbackFor = Exception.class)
    public int batchScore(Long sessionId, AttendanceBatchScoreDTO dto) {
        AttendanceSession session = requireEditableSession(sessionId);
        classAccessService.checkAttendanceAccess(session.getClassId());
        int success = 0;
        for (Long studentId : dto.getStudentIds()) {
            Student student = requireStudentInClass(studentId, session.getClassId());
            if (student.getIsAssistant() != null && student.getIsAssistant() == 1) {
                continue;
            }
            Long existing = recordMapper.selectCount(
                    new LambdaQueryWrapper<AttendanceRecord>()
                            .eq(AttendanceRecord::getSessionId, sessionId)
                            .eq(AttendanceRecord::getStudentId, studentId));
            if (existing != null && existing > 0) {
                continue;
            }
            AttendanceRecordSaveDTO single = new AttendanceRecordSaveDTO();
            single.setAttendanceType(dto.getAttendanceType() == null ? "NORMAL" : dto.getAttendanceType());
            single.setScore(dto.getScore());
            single.setRemark(dto.getRemark() == null ? "" : dto.getRemark());
            saveRecord(sessionId, studentId, single);
            success++;
        }
        operationLogService.record("ATTENDANCE", "BATCH_SCORE", "ATTENDANCE_SESSION", sessionId,
                "一键打分，成功 " + success + " 人，跳过 "
                        + (dto.getStudentIds().size() - success) + " 人，分数=" + dto.getScore(),
                null, dto);
        return success;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteRecord(Long sessionId, Long studentId) {
        AttendanceSession session = requireEditableSession(sessionId);
        classAccessService.checkAttendanceAccess(session.getClassId());
        Student student = requireStudentInClass(studentId, session.getClassId());
        preventAssistantScoring(student);
        AttendanceRecord before = recordMapper.selectOne(
                new LambdaQueryWrapper<AttendanceRecord>()
                        .eq(AttendanceRecord::getSessionId, sessionId)
                        .eq(AttendanceRecord::getStudentId, studentId));
        recordMapper.physicalDelete(sessionId, studentId);
        if (before != null) {
            operationLogService.record("ATTENDANCE", "DELETE", "ATTENDANCE_RECORD", before.getId(),
                    "删除学生考勤记录", before, null);
        }
    }

    private Classes requireClass(Long classId) {
        Classes classes = classesMapper.selectById(classId);
        if (classes == null) {
            throw new BusinessException("班级不存在");
        }
        return classes;
    }

    private AttendanceSession requireSession(Long sessionId) {
        AttendanceSession session = sessionMapper.selectById(sessionId);
        if (session == null) {
            throw new BusinessException("劳动课课次不存在");
        }
        return session;
    }

    private AttendanceSession requireEditableSession(Long sessionId) {
        AttendanceSession session = requireSession(sessionId);
        AttendanceSessionPolicy.SessionWindow window = sessionPolicy.evaluate(session);
        if (!window.editable()) {
            throw new BusinessException(ResultCode.DATA_SEALED, window.lockReason());
        }
        return session;
    }

    private Student requireStudentInClass(Long studentId, Long classId) {
        Student student = studentMapper.selectById(studentId);
        if (student == null) {
            throw new BusinessException("学生不存在");
        }
        if (!classId.equals(student.getClassId())) {
            throw new BusinessException("该学生不属于此课次对应班级");
        }
        return student;
    }

    private void preventAssistantScoring(Student student) {
        if (student.getIsAssistant() != null && student.getIsAssistant() == 1) {
            throw new BusinessException(ResultCode.PERMISSION_DENIED, "助教身份学生不参与考勤打分");
        }
    }

    private void checkSessionWeekUnique(Long classId, Integer weekNo, Long excludeId) {
        LambdaQueryWrapper<AttendanceSession> wrapper = new LambdaQueryWrapper<AttendanceSession>()
                .eq(AttendanceSession::getClassId, classId)
                .eq(AttendanceSession::getWeekNo, weekNo);
        if (excludeId != null) {
            wrapper.ne(AttendanceSession::getId, excludeId);
        }
        Long count = sessionMapper.selectCount(wrapper);
        if (count != null && count > 0) {
            throw new BusinessException("该班级周" + weekNo + "的劳动课已存在");
        }
    }

    private void validateFlag(Integer value, String fieldName) {
        if (value != null && value != 0 && value != 1) {
            throw new BusinessException(fieldName + "只能是0或1");
        }
    }

    private AttendanceSessionVO toSessionVO(AttendanceSession entity, String className) {
        AttendanceSessionVO vo = new AttendanceSessionVO();
        BeanUtils.copyProperties(entity, vo);
        vo.setClassName(className);
        return vo;
    }

    private void applyWindow(AttendanceSessionVO vo,
                             AttendanceSessionPolicy.SessionWindow window) {
        vo.setEditable(window.editable());
        vo.setLockReason(window.lockReason());
        vo.setSealDate(window.sealDate());
        vo.setSealDays(window.sealDays());
    }

    private AttendanceRecordVO toRecordVO(Long sessionId, Student student, AttendanceRecord record) {
        AttendanceRecordVO vo = new AttendanceRecordVO();
        vo.setSessionId(sessionId);
        vo.setStudentId(student.getId());
        vo.setStudentNo(student.getStudentId());
        vo.setStudentName(student.getName());
        vo.setStudentNoInClass(student.getStudentNoInClass());
        vo.setIsAssistant(student.getIsAssistant());
        if (record != null) {
            vo.setId(record.getId());
            vo.setAttendanceType(record.getAttendanceType());
            vo.setScore(record.getScore());
            vo.setRemark(record.getRemark());
            vo.setRecordedBy(record.getRecordedBy());
            vo.setUpdatedAt(record.getUpdatedAt());
        }
        return vo;
    }
}
