package com.labor.management.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.labor.management.dto.StudentCreateDTO;
import com.labor.management.dto.StudentQueryDTO;
import com.labor.management.dto.StudentUpdateDTO;
import com.labor.management.vo.StudentVO;

import java.util.List;

/**
 * 学生 Service 接口
 *
 * <p>查询/单条操作方法均带 scopeClassIds 参数实现数据隔离：</p>
 * <ul>
 *   <li>scopeClassIds = null：不限制（超级管理员）</li>
 *   <li>scopeClassIds = 非 null List：限制学生 class_id 必须在该列表内（教师）</li>
 * </ul>
 */
public interface StudentService {

    /**
     * 分页查询学生（支持关键词模糊搜索、班级筛选，关联班级表获取班级名称）
     *
     * @param scopeClassIds 数据范围班级ID（null=不限制）
     */
    IPage<StudentVO> pageQuery(StudentQueryDTO queryDTO, List<Long> scopeClassIds);

    /**
     * 根据 ID 查询学生
     *
     * @param scopeClassIds 数据范围班级ID（null=不限制）；非 null 时校验学生 class_id 必须在范围内
     */
    StudentVO getById(Long id, List<Long> scopeClassIds);

    /**
     * 新增学生（校验 studentId 唯一性）
     *
     * @param scopeClassIds 数据范围班级ID（null=不限制）；非 null 时校验 dto.classId 必须在范围内
     */
    void create(StudentCreateDTO dto, List<Long> scopeClassIds);

    /**
     * 修改学生（校验 studentId 唯一性，排除自身）
     *
     * @param scopeClassIds 数据范围班级ID（null=不限制）；非 null 时校验 dto.classId 必须在范围内
     */
    void update(StudentUpdateDTO dto, List<Long> scopeClassIds);

    /**
     * 逻辑删除学生
     *
     * @param scopeClassIds 数据范围班级ID（null=不限制）；非 null 时校验学生 class_id 必须在范围内
     */
    void deleteById(Long id, List<Long> scopeClassIds);
}
