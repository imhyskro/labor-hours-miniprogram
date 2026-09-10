package com.labor.management.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.labor.management.entity.Classes;
import com.labor.management.entity.Student;
import com.labor.management.exception.BusinessException;
import com.labor.management.mapper.ClassesMapper;
import com.labor.management.mapper.StudentMapper;
import com.labor.management.service.AssistantAssignmentService;
import com.labor.management.vo.MasterListViewVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 助教分配 Service 实现
 *
 * <p>一个助教最多负责一个班级（assigned_class_id 单值），但一个班级可分配多个助教（无限制）。</p>
 */
@Service
@RequiredArgsConstructor
public class AssistantAssignmentServiceImpl implements AssistantAssignmentService {

    private final StudentMapper studentMapper;
    private final ClassesMapper classesMapper;

    @Override
    public List<MasterListViewVO> getUnassignedAssistants() {
        return studentMapper.selectUnassignedAssistants();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignToClass(Long studentId, Long classId) {
        Student student = studentMapper.selectById(studentId);
        if (student == null) {
            throw new BusinessException("学生/助教不存在");
        }
        if (student.getIsAssistant() == null || student.getIsAssistant() != 1) {
            throw new BusinessException("该学生不是助教，无法分配班级");
        }
        Classes classes = classesMapper.selectById(classId);
        if (classes == null) {
            throw new BusinessException("班级不存在");
        }
        // 不限制班级唯一性：同一班级可分配多个助教，仅更新 assigned_class_id
        studentMapper.update(null, new LambdaUpdateWrapper<Student>()
                .eq(Student::getId, studentId)
                .set(Student::getAssignedClassId, classId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unassign(Long studentId) {
        Student student = studentMapper.selectById(studentId);
        if (student == null) {
            throw new BusinessException("学生/助教不存在");
        }
        if (student.getIsAssistant() == null || student.getIsAssistant() != 1) {
            throw new BusinessException("该学生不是助教");
        }
        if (student.getAssignedClassId() == null) {
            throw new BusinessException("该助教尚未分配班级");
        }
        // 用 UpdateWrapper 显式 set null（updateById 默认忽略 null 字段）
        studentMapper.update(null, new LambdaUpdateWrapper<Student>()
                .eq(Student::getId, studentId)
                .set(Student::getAssignedClassId, null));
    }
}
