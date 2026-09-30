package com.labor.management.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.labor.management.entity.Student;
import com.labor.management.vo.AssistantVO;
import com.labor.management.vo.MasterListViewVO;
import com.labor.management.vo.StudentVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 学生 Mapper
 */
@Mapper
public interface StudentMapper extends BaseMapper<Student> {

    /**
     * 分页查询学生（关联班级表获取班级名称，支持关键词模糊搜索、班级筛选）
     *
     * @param scopeClassIds 数据范围班级ID列表（null=不限制；非空列表=限定 class_id IN 列表）
     */
    IPage<StudentVO> selectStudentPage(IPage<StudentVO> page,
                                       @Param("keyword") String keyword,
                                       @Param("classId") Long classId,
                                       @Param("scopeClassIds") List<Long> scopeClassIds);

    /**
     * 总表分页查询（公司 → 班级 W-S-E → 班内编号）
     *
     * @param page      分页参数
     * @param keyword   关键词（学号或姓名模糊搜索）
     * @param companyId 公司ID（可选）
     * @param classId   班级ID（可选）
     * @param identity  身份筛选：null=全部, "STUDENT"=普通学生, "ASSISTANT"=助教
     * @param scopeClassIds 数据范围班级ID列表（null=不限制；非空列表=限定 class_id IN 列表）
     */
    IPage<MasterListViewVO> selectMasterListPage(IPage<MasterListViewVO> page,
                                                 @Param("keyword") String keyword,
                                                 @Param("companyId") Long companyId,
                                                 @Param("classId") Long classId,
                                                 @Param("identity") String identity,
                                                 @Param("scopeClassIds") List<Long> scopeClassIds);

    /**
     * 助教分页查询（is_assistant=1，含负责班级数量/聚合名）
     */
    IPage<AssistantVO> selectAssistantPage(IPage<AssistantVO> page,
                                           @Param("keyword") String keyword);

    /**
     * 查询尚未分配任何负责班级的助教列表（assistant_class 无记录）
     */
    List<MasterListViewVO> selectUnassignedAssistants();
}
