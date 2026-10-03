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

    /**
     * 物理删除某教师在本公司范围内的负责班级关联（不影响其他公司分配）
     *
     * <p>用于权限分配 Tab 按公司维度增量覆盖：只清掉该教师在 companyId 范围内的关联，
     * 其他公司的分配保留。teacher_class 表本身无 company_id 列，通过 classes 子查询限定范围。
     * 子查询不引用被删表本身，MySQL 允许。</p>
     */
    @Delete("DELETE FROM teacher_class WHERE user_id = #{userId} " +
            "AND class_id IN (SELECT id FROM classes WHERE company_id = #{companyId})")
    int physicalDeleteByUserAndCompany(@Param("userId") Long userId,
                                       @Param("companyId") Long companyId);
}
