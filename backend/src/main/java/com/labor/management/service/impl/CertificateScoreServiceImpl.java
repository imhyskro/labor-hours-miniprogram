package com.labor.management.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.labor.management.dto.CertificateScoreSaveDTO;
import com.labor.management.entity.CertificateScore;
import com.labor.management.entity.Classes;
import com.labor.management.entity.Student;
import com.labor.management.exception.BusinessException;
import com.labor.management.mapper.CertificateScoreMapper;
import com.labor.management.mapper.ClassesMapper;
import com.labor.management.mapper.StudentMapper;
import com.labor.management.service.CertificateScoreService;
import com.labor.management.service.ClassAccessService;
import com.labor.management.util.SecurityUtil;
import com.labor.management.vo.CertificateScoreVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 换证考试成绩服务实现。 */
@Service
@RequiredArgsConstructor
public class CertificateScoreServiceImpl implements CertificateScoreService {

    private final CertificateScoreMapper scoreMapper;
    private final StudentMapper studentMapper;
    private final ClassesMapper classesMapper;
    private final ClassAccessService classAccessService;

    @Override
    public List<CertificateScoreVO> listScores(Long classId, String academicYear, Integer semester) {
        validateTerm(academicYear, semester);
        requireClass(classId);
        classAccessService.checkTeacherAccess(classId);

        List<Student> students = studentMapper.selectList(
                new LambdaQueryWrapper<Student>()
                        .eq(Student::getClassId, classId)
                        .eq(Student::getStatus, 1)
                        .orderByAsc(Student::getStudentNoInClass)
                        .orderByAsc(Student::getId));
        List<CertificateScore> scores = scoreMapper.selectList(
                new LambdaQueryWrapper<CertificateScore>()
                        .eq(CertificateScore::getClassId, classId)
                        .eq(CertificateScore::getAcademicYear, academicYear.trim())
                        .eq(CertificateScore::getSemester, semester));
        Map<Long, CertificateScore> scoreByStudent = new HashMap<>();
        for (CertificateScore score : scores) {
            scoreByStudent.put(score.getStudentId(), score);
        }
        return students.stream()
                .map(student -> toVO(classId, academicYear.trim(), semester,
                        student, scoreByStudent.get(student.getId())))
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveScore(Long studentId, CertificateScoreSaveDTO dto) {
        validateTerm(dto.getAcademicYear(), dto.getSemester());
        requireClass(dto.getClassId());
        classAccessService.checkTeacherAccess(dto.getClassId());
        Student student = requireStudentInClass(studentId, dto.getClassId());

        String academicYear = dto.getAcademicYear().trim();
        CertificateScore entity = scoreMapper.selectOne(
                new LambdaQueryWrapper<CertificateScore>()
                        .eq(CertificateScore::getStudentId, student.getId())
                        .eq(CertificateScore::getAcademicYear, academicYear)
                        .eq(CertificateScore::getSemester, dto.getSemester()));
        if (entity == null) {
            entity = new CertificateScore();
            entity.setStudentId(student.getId());
            entity.setAcademicYear(academicYear);
            entity.setSemester(dto.getSemester());
        }
        entity.setClassId(dto.getClassId());
        entity.setFinalScore(dto.getFinalScore());
        entity.setRemark(dto.getRemark());
        entity.setRecordedBy(SecurityUtil.getCurrentUserId());
        if (entity.getId() == null) {
            scoreMapper.insert(entity);
        } else {
            scoreMapper.updateById(entity);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteScore(Long studentId, Long classId, String academicYear, Integer semester) {
        validateTerm(academicYear, semester);
        requireClass(classId);
        classAccessService.checkTeacherAccess(classId);
        requireStudentInClass(studentId, classId);

        CertificateScore score = scoreMapper.selectOne(
                new LambdaQueryWrapper<CertificateScore>()
                        .eq(CertificateScore::getStudentId, studentId)
                        .eq(CertificateScore::getClassId, classId)
                        .eq(CertificateScore::getAcademicYear, academicYear.trim())
                        .eq(CertificateScore::getSemester, semester));
        if (score != null) {
            scoreMapper.physicalDelete(studentId, academicYear.trim(), semester);
        }
    }

    private void requireClass(Long classId) {
        Classes classes = classesMapper.selectById(classId);
        if (classes == null) {
            throw new BusinessException("班级不存在");
        }
    }

    private Student requireStudentInClass(Long studentId, Long classId) {
        Student student = studentMapper.selectById(studentId);
        if (student == null) {
            throw new BusinessException("学生不存在");
        }
        if (!classId.equals(student.getClassId())) {
            throw new BusinessException("该学生不属于所选班级");
        }
        return student;
    }

    private void validateTerm(String academicYear, Integer semester) {
        if (academicYear == null || academicYear.isBlank()) {
            throw new BusinessException("学年不能为空");
        }
        if (semester == null || (semester != 1 && semester != 2)) {
            throw new BusinessException("学期只能是1或2");
        }
    }

    private CertificateScoreVO toVO(Long classId, String academicYear, Integer semester,
                                    Student student, CertificateScore score) {
        CertificateScoreVO vo = new CertificateScoreVO();
        vo.setStudentId(student.getId());
        vo.setStudentNo(student.getStudentId());
        vo.setStudentName(student.getName());
        vo.setStudentNoInClass(student.getStudentNoInClass());
        vo.setClassId(classId);
        vo.setAcademicYear(academicYear);
        vo.setSemester(semester);
        if (score != null) {
            vo.setId(score.getId());
            vo.setFinalScore(score.getFinalScore());
            vo.setRemark(score.getRemark());
            vo.setRecordedBy(score.getRecordedBy());
            vo.setUpdatedAt(score.getUpdatedAt());
        }
        return vo;
    }
}
