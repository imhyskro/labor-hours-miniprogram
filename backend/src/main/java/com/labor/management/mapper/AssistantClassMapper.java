package com.labor.management.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.labor.management.entity.AssistantClass;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 助教负责班级关联 Mapper
 *
 * <p>关联表仅表达关系，重新分配时采用物理删除，避免 (assistant_student_id, class_id)
 * 唯一键与历史逻辑删除记录冲突。</p>
 */
@Mapper
public interface AssistantClassMapper extends BaseMapper<AssistantClass> {

    /** 物理删除某助教的全部负责班级关联 */
    @Delete("DELETE FROM assistant_class WHERE assistant_student_id = #{assistantStudentId}")
    int physicalDeleteByAssistant(@Param("assistantStudentId") Long assistantStudentId);

    /**
     * 物理删除某助教在本公司范围内的负责班级关联（不影响其他公司分配）
     *
     * <p>用于权限分配 Tab 按公司维度增量覆盖：只清掉该助教在 companyId 范围内的关联，
     * 其他公司的分配保留。assistant_class 表本身无 company_id 列，通过 classes 子查询限定范围。</p>
     */
    @Delete("DELETE FROM assistant_class WHERE assistant_student_id = #{assistantStudentId} " +
            "AND class_id IN (SELECT id FROM classes WHERE company_id = #{companyId})")
    int physicalDeleteByAssistantAndCompany(@Param("assistantStudentId") Long assistantStudentId,
                                            @Param("companyId") Long companyId);
}
