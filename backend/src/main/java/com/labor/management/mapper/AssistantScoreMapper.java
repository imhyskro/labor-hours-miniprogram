package com.labor.management.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.labor.management.entity.AssistantScore;
import com.labor.management.vo.AssistantScoreRowVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;
import java.util.List;

/**
 * 助教打分 Mapper
 *
 * <p>打分维度：助教 × 班级 × 第几次课。唯一键 uk_assistant_class_session
 * (assistant_student_id, class_id, session_no) 不含 deleted，已删除记录仍占键，
 * 重复录入需复活而非 INSERT。</p>
 */
@Mapper
public interface AssistantScoreMapper extends BaseMapper<AssistantScore> {

    /**
     * 按「助教 + 班级 + 第几次课」查询打分记录（含已逻辑删除的记录，绕过 @TableLogic）
     */
    AssistantScore selectByAssistantClassSessionIncludeDeleted(@Param("assistantStudentId") Long assistantStudentId,
                                                                @Param("classId") Long classId,
                                                                @Param("sessionNo") Integer sessionNo);

    /**
     * 复活已逻辑删除的打分记录并刷新分数（原生 SQL 绕过 @TableLogic）
     */
    @Update("UPDATE assistant_score SET deleted = 0, score = #{score}, updated_at = NOW() WHERE id = #{id}")
    int reviveById(@Param("id") Long id, @Param("score") BigDecimal score);

    /**
     * 分页查询当前用户范围内的助教打分行（含主班 scoringClassId 与班级详情）。
     *
     * <p>过滤逻辑：JOIN assistant_class，按 scopeClassIds 过滤；
     * scopeClassIds == null（超管）不限制；空 List（教师无负责班）返回空；
     * 每助教取 MIN(class_id) 作为主班 scoringClassId（"一助教一行"去重）。</p>
     */
    List<AssistantScoreRowVO> pageScopedAssistants(@Param("scopeClassIds") List<Long> scopeClassIds,
                                                    @Param("keyword") String keyword,
                                                    @Param("offset") int offset,
                                                    @Param("size") int size);

    /**
     * 计数当前用户范围内的助教数（去重）
     */
    long countScopedAssistants(@Param("scopeClassIds") List<Long> scopeClassIds,
                               @Param("keyword") String keyword);
}
