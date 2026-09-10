package com.labor.management.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.labor.management.dto.ClassCreateDTO;
import com.labor.management.dto.ClassUpdateDTO;
import com.labor.management.entity.Classes;
import com.labor.management.entity.Student;
import com.labor.management.exception.BusinessException;
import com.labor.management.mapper.ClassesMapper;
import com.labor.management.mapper.StudentMapper;
import com.labor.management.service.ClassesService;
import com.labor.management.vo.ClassVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 班级 Service 实现
 */
@Service
@RequiredArgsConstructor
public class ClassesServiceImpl implements ClassesService {

    private final ClassesMapper classesMapper;
    private final StudentMapper studentMapper;

    @Override
    public IPage<ClassVO> pageQuery(Integer page, Integer size) {
        Page<Classes> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Classes> wrapper = new LambdaQueryWrapper<Classes>()
                .orderByDesc(Classes::getCreatedAt);
        IPage<Classes> entityPage = classesMapper.selectPage(pageParam, wrapper);

        // 转换为 VO 并补充学生数
        Page<ClassVO> resultPage = new Page<>(entityPage.getCurrent(), entityPage.getSize(), entityPage.getTotal());
        List<ClassVO> voList = entityPage.getRecords().stream()
                .map(this::toVO)
                .collect(Collectors.toList());
        resultPage.setRecords(voList);
        return resultPage;
    }

    @Override
    public ClassVO getById(Long id) {
        Classes entity = classesMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException("班级不存在");
        }
        return toVO(entity);
    }

    @Override
    public void create(ClassCreateDTO dto) {
        // 校验 classCode 唯一
        Long count = classesMapper.selectCount(
                new LambdaQueryWrapper<Classes>()
                        .eq(Classes::getClassCode, dto.getClassCode())
        );
        if (count != null && count > 0) {
            throw new BusinessException("班级编号已存在");
        }

        Classes entity = new Classes();
        BeanUtils.copyProperties(dto, entity);
        entity.setStatus(1);
        classesMapper.insert(entity);
    }

    @Override
    public void update(ClassUpdateDTO dto) {
        Classes existing = classesMapper.selectById(dto.getId());
        if (existing == null) {
            throw new BusinessException("班级不存在");
        }

        // 校验 classCode 唯一，排除自身
        Long count = classesMapper.selectCount(
                new LambdaQueryWrapper<Classes>()
                        .eq(Classes::getClassCode, dto.getClassCode())
                        .ne(Classes::getId, dto.getId())
        );
        if (count != null && count > 0) {
            throw new BusinessException("班级编号已存在");
        }

        BeanUtils.copyProperties(dto, existing);
        classesMapper.updateById(existing);
    }

    @Override
    public void deleteById(Long id) {
        Classes existing = classesMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException("班级不存在");
        }
        classesMapper.deleteById(id);
    }

    @Override
    public List<ClassVO> listAll() {
        List<Classes> list = classesMapper.selectList(
                new LambdaQueryWrapper<Classes>()
                        .eq(Classes::getStatus, 1)
                        .orderByAsc(Classes::getClassName)
        );
        return list.stream().map(this::toVO).collect(Collectors.toList());
    }

    /**
     * 实体转 VO，并统计学生数
     */
    private ClassVO toVO(Classes entity) {
        ClassVO vo = new ClassVO();
        BeanUtils.copyProperties(entity, vo);

        // 统计该班级学生数
        Long studentCount = studentMapper.selectCount(
                new LambdaQueryWrapper<Student>()
                        .eq(Student::getClassId, entity.getId())
        );
        vo.setStudentCount(studentCount);
        return vo;
    }
}
