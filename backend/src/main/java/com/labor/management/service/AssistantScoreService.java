package com.labor.management.service;

import com.labor.management.dto.ScoreSaveDTO;
import com.labor.management.vo.AssistantScoreTableVO;

import java.util.List;

/**
 * 助教打分 Service
 *
 * <p>按 助教 × 每次课 记录分数（每次满分 10 分）；上课次数（分数列数）可由教师手动调整。</p>
 */
public interface AssistantScoreService {

    /**
     * 查询打分表：上课次数 + 当前页助教行（含各次课分数）
     *
     * @param scopeClassIds 数据范围班级ID（null=不限制；非 null 时仅返回范围内的助教）
     */
    AssistantScoreTableVO getScoreTable(Integer page, Integer size, String keyword, List<Long> scopeClassIds);

    /**
     * 批量保存分数（存在则更新/复活，不存在则新建）
     *
     * @param scopeClassIds 数据范围班级ID（null=不限制；非 null 时仅允许给范围内助教打分，
     *                      越权打分抛 BusinessException）
     */
    void saveScores(ScoreSaveDTO dto, List<Long> scopeClassIds);

    /**
     * 调整上课次数（打分列数）；次数减少时逻辑删除超出的分数记录
     */
    void updateSessionCount(Integer sessionCount);
}
