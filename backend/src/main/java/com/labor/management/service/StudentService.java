package com.labor.management.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.labor.management.dto.StudentCreateDTO;
import com.labor.management.dto.StudentQueryDTO;
import com.labor.management.dto.StudentUpdateDTO;
import com.labor.management.vo.StudentVO;

/**
 * 学生 Service 接口
 */
public interface StudentService {

    /**
     * 分页查询学生（支持关键词模糊搜索、班级筛选，关联班级表获取班级名称）
     */
    IPage<StudentVO> pageQuery(StudentQueryDTO queryDTO);

    /**
     * 根据 ID 查询学生
     */
    StudentVO getById(Long id);

    /**
     * 新增学生（校验 studentId 唯一性）
     */
    void create(StudentCreateDTO dto);

    /**
     * 修改学生（校验 studentId 唯一性，排除自身）
     */
    void update(StudentUpdateDTO dto);

    /**
     * 逻辑删除学生
     */
    void deleteById(Long id);
}
