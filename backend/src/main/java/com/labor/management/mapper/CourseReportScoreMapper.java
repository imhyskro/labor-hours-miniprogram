package com.labor.management.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.labor.management.entity.CourseReportScore;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;

/**
 * 课程报告分数 Mapper
 *
 * <p>唯一键 uk_student (student_id) 不含 deleted，导入时需复活已删记录（仿 student 表）。</p>
 */
@Mapper
public interface CourseReportScoreMapper extends BaseMapper<CourseReportScore> {

    /**
     * 按学生ID查询课程报告分数（含已逻辑删除的记录，绕过 @TableLogic 自动过滤）
     */
    @Select("SELECT * FROM course_report_score WHERE student_id = #{studentId} LIMIT 1")
    CourseReportScore selectByStudentIdIncludeDeleted(@Param("studentId") Long studentId);

    /**
     * 复活已逻辑删除的记录并刷新分数（原生 SQL 绕过 @TableLogic）
     */
    @Update("UPDATE course_report_score SET deleted = 0, score = #{score}, updated_at = NOW() WHERE id = #{id}")
    int reviveById(@Param("id") Long id, @Param("score") BigDecimal score);
}
