package com.labor.management.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.labor.management.entity.AttendanceScore;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 考勤分明细 Mapper
 *
 * <p>由小程序端助教打分同步写入；本次仅提供读取能力供成绩汇总使用。</p>
 * <p>唯一键 uk_student_session (student_id, session_no) 不含 deleted。</p>
 */
@Mapper
public interface AttendanceScoreMapper extends BaseMapper<AttendanceScore> {

    /**
     * 批量查询多个学生的考勤分明细（成绩汇总用，仅未删记录）
     *
     * @param studentIds 学生ID列表；为空时返回空列表
     */
    @Select({
            "<script>",
            "SELECT student_id, session_no, score FROM attendance_score WHERE deleted = 0",
            "  AND student_id IN",
            "  <foreach collection='studentIds' item='sid' open='(' separator=',' close=')'>#{sid}</foreach>",
            "</script>"
    })
    List<AttendanceScore> selectByStudentIds(@Param("studentIds") List<Long> studentIds);

    /**
     * 聚合查询：按学生分组求考勤分总和（成绩汇总用，仅未删记录）
     *
     * <p>返回字段：studentId, totalScore。归一化由 Service 层按 session_count 计算。</p>
     */
    @Select({
            "<script>",
            "SELECT student_id AS studentId, SUM(score) AS totalScore FROM attendance_score WHERE deleted = 0",
            "  AND student_id IN",
            "  <foreach collection='studentIds' item='sid' open='(' separator=',' close=')'>#{sid}</foreach>",
            "  GROUP BY student_id",
            "</script>"
    })
    List<Map<String, Object>> sumByStudent(@Param("studentIds") List<Long> studentIds);
}
