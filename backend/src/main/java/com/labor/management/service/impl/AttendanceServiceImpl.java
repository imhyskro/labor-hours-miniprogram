package com.labor.management.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.labor.management.common.ResultCode;
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
import com.labor.management.service.ClassAccessService;
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

/** 劳动课课次及考勤记录服务实现。 */
@Service
@RequiredArgsConstructor
public class AttendanceServiceImpl implements AttendanceService {

    private final AttendanceSessionMapper sessionMapper;
    private final AttendanceRecordMapper recordMapper;
    private final ClassesMapper classesMapper;
    private final StudentMapper studentMapper;
    private final ClassAccessService classAccessService;

    @Override
    public List<AttendanceSessionVO> listSessions(Long classId) {
        Classes classes = requireClass(classId);
        classAccessService.checkAttendanceAccess(classId);
        return sessionMapper.selectList(
                        new LambdaQueryWrapper<AttendanceSession>()
                                .eq(AttendanceSession::getClassId, classId)
                                .orderByAsc(AttendanceSession::getWeekNo))
                .stream()
                .map(entity -> toSessionVO(entity, classes.getClassName()))
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
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateSession(Long sessionId, AttendanceSessionUpdateDTO dto) {
        AttendanceSession entity = requireSession(sessionId);
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
        preventAssistantSelfScoring(student.getId());

        String type = dto.getAttendanceType().trim().toUpperCase(Locale.ROOT);
        if (!List.of("NORMAL", "J", "K").contains(type)) {
            throw new BusinessException("考勤类型只能是 NORMAL、J 或 K");
        }
        AttendanceRecord entity = recordMapper.selectOne(
                new LambdaQueryWrapper<AttendanceRecord>()
                        .eq(AttendanceRecord::getSessionId, sessionId)
                        .eq(AttendanceRecord::getStudentId, studentId));
        if (entity == null) {
            entity = new AttendanceRecord();
            entity.setSessionId(sessionId);
            entity.setStudentId(studentId);
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
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteRecord(Long sessionId, Long studentId) {
        AttendanceSession session = requireEditableSession(sessionId);
        classAccessService.checkAttendanceAccess(session.getClassId());
        Student student = requireStudentInClass(studentId, session.getClassId());
        preventAssistantSelfScoring(student.getId());
        recordMapper.physicalDelete(sessionId, studentId);
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
        if (session.getStatus() != null && session.getStatus() == 0) {
            throw new BusinessException(ResultCode.DATA_SEALED);
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

    private void preventAssistantSelfScoring(Long targetStudentId) {
        Long assistantStudentId = classAccessService.getCurrentAssistantStudentId();
        if (assistantStudentId != null && assistantStudentId.equals(targetStudentId)) {
            throw new BusinessException(ResultCode.PERMISSION_DENIED, "助教不能给自己登记考勤或打分");
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
            throw new BusinessException("该班级第 " + weekNo + " 周的劳动课已存在");
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

    private AttendanceRecordVO toRecordVO(Long sessionId, Student student, AttendanceRecord record) {
        AttendanceRecordVO vo = new AttendanceRecordVO();
        vo.setSessionId(sessionId);
        vo.setStudentId(student.getId());
        vo.setStudentNo(student.getStudentId());
        vo.setStudentName(student.getName());
        vo.setStudentNoInClass(student.getStudentNoInClass());
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
