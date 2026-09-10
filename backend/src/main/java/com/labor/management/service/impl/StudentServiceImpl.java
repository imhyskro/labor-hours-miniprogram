package com.labor.management.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.labor.management.dto.StudentCreateDTO;
import com.labor.management.dto.StudentQueryDTO;
import com.labor.management.dto.StudentUpdateDTO;
import com.labor.management.entity.Classes;
import com.labor.management.entity.Student;
import com.labor.management.exception.BusinessException;
import com.labor.management.mapper.ClassesMapper;
import com.labor.management.mapper.StudentMapper;
import com.labor.management.service.StudentService;
import com.labor.management.vo.StudentVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 学生 Service 实现
 */
@Service
@RequiredArgsConstructor
public class StudentServiceImpl implements StudentService {

    private final StudentMapper studentMapper;
    private final ClassesMapper classesMapper;

    @Override
    public IPage<StudentVO> pageQuery(StudentQueryDTO queryDTO) {
        Page<StudentVO> pageParam = new Page<>(queryDTO.getPage(), queryDTO.getSize());
        String keyword = StringUtils.hasText(queryDTO.getKeyword())
                ? queryDTO.getKeyword().trim()
                : null;
        Long classId = queryDTO.getClassId();
        return studentMapper.selectStudentPage(pageParam, keyword, classId);
    }

    @Override
    public StudentVO getById(Long id) {
        Student entity = studentMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException("学生不存在");
        }
        return toVO(entity);
    }

    @Override
    public void create(StudentCreateDTO dto) {
        // 校验 studentId 唯一
        Long count = studentMapper.selectCount(
                new LambdaQueryWrapper<Student>()
                        .eq(Student::getStudentId, dto.getStudentId())
        );
        if (count != null && count > 0) {
            throw new BusinessException("学号已存在");
        }

        // 校验班级存在
        Classes classes = classesMapper.selectById(dto.getClassId());
        if (classes == null) {
            throw new BusinessException("班级不存在");
        }

        Student entity = new Student();
        BeanUtils.copyProperties(dto, entity);
        entity.setStatus(1);
        studentMapper.insert(entity);
    }

    @Override
    public void update(StudentUpdateDTO dto) {
        Student existing = studentMapper.selectById(dto.getId());
        if (existing == null) {
            throw new BusinessException("学生不存在");
        }

        // 校验 studentId 唯一，排除自身
        Long count = studentMapper.selectCount(
                new LambdaQueryWrapper<Student>()
                        .eq(Student::getStudentId, dto.getStudentId())
                        .ne(Student::getId, dto.getId())
        );
        if (count != null && count > 0) {
            throw new BusinessException("学号已存在");
        }

        // 校验班级存在
        Classes classes = classesMapper.selectById(dto.getClassId());
        if (classes == null) {
            throw new BusinessException("班级不存在");
        }

        BeanUtils.copyProperties(dto, existing);
        studentMapper.updateById(existing);
    }

    @Override
    public void deleteById(Long id) {
        Student existing = studentMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException("学生不存在");
        }
        studentMapper.deleteById(id);
    }

    /**
     * 实体转 VO，补充班级名称
     */
    private StudentVO toVO(Student entity) {
        StudentVO vo = new StudentVO();
        BeanUtils.copyProperties(entity, vo);
        if (entity.getClassId() != null) {
            Classes classes = classesMapper.selectById(entity.getClassId());
            if (classes != null) {
                vo.setClassName(classes.getClassName());
            }
        }
        return vo;
    }
}
