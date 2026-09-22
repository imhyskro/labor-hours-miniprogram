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
}
