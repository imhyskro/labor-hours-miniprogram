package com.labor.management.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.labor.management.entity.TeacherClass;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 教师负责班级关联 Mapper
 *
 * <p>关联表仅表达关系，重新分配时采用物理删除，避免 (user_id, class_id)
 * 唯一键与历史逻辑删除记录冲突。</p>
 */
@Mapper
public interface TeacherClassMapper extends BaseMapper<TeacherClass> {

    /** 物理删除某教师的全部负责班级关联 */
    @Delete("DELETE FROM teacher_class WHERE user_id = #{userId}")
    int physicalDeleteByUser(@Param("userId") Long userId);
}
