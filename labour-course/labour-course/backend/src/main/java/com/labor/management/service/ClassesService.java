package com.labor.management.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.labor.management.dto.ClassCreateDTO;
import com.labor.management.dto.ClassUpdateDTO;
import com.labor.management.entity.Classes;
import com.labor.management.vo.ClassStudentVO;
import com.labor.management.vo.ClassVO;

import java.util.List;

/**
 * 班级 Service 接口
 *
 * <p>查询/单条操作方法均带 scopeClassIds 参数实现数据隔离：</p>
 * <ul>
 *   <li>scopeClassIds = null：不限制（超级管理员）</li>
 *   <li>scopeClassIds = 非 null List：限制为这些班级（教师）</li>
 * </ul>
 */
public interface ClassesService {

    /**
     * 分页查询班级
     *
     * @param scopeClassIds 数据范围班级ID（null=不限制）
     */
    IPage<ClassVO> pageQuery(Integer page, Integer size, List<Long> scopeClassIds);

    /**
     * 根据 ID 查询班级
     *
     * @param scopeClassIds 数据范围班级ID（null=不限制）；非 null 时校验 id 必须在范围内
     */
    ClassVO getById(Long id, List<Long> scopeClassIds);

    /**
     * 新增班级（公司+周次+节次，编码与名称自动生成）
     */
    void create(ClassCreateDTO dto);

    /**
     * 修改班级（周次/节次）
     *
     * @param scopeClassIds 数据范围班级ID（null=不限制）；非 null 时校验 id 必须在范围内
     */
    void update(ClassUpdateDTO dto, List<Long> scopeClassIds);

    /**
     * 逻辑删除班级
     *
     * @param scopeClassIds 数据范围班级ID（null=不限制）；非 null 时校验 id 必须在范围内
     */
    void deleteById(Long id, List<Long> scopeClassIds);

    /**
     * 查询所有班级（含公司名，按公司、周次、节次排序，下拉/筛选用）
     *
     * @param scopeClassIds 数据范围班级ID（null=不限制）
     */
    List<ClassVO> listAll(List<Long> scopeClassIds);

    /**
     * 查询某公司下的所有班级（按周次-开始节次-结束节次排序，含学生数）
     *
     * @param scopeClassIds 数据范围班级ID（null=不限制）
     */
    List<ClassVO> listByCompany(Long companyId, List<Long> scopeClassIds);

    /**
     * 查询某班级的学生列表（按班级内编号排序）
     *
     * @param scopeClassIds 数据范围班级ID（null=不限制）；非 null 时校验 classId 必须在范围内
     */
    List<ClassStudentVO> getClassStudents(Long classId, List<Long> scopeClassIds);

    /**
     * 查找班级，不存在则创建（导入用）。
     * 班级唯一 = 公司 + 周次 + 开始节次 + 结束节次。
     */
    Classes findOrCreateClass(Long companyId, Integer week, Integer startSession, Integer endSession);
}
