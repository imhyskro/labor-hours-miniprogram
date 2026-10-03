package com.labor.management.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.labor.management.dto.ScoreSingleSaveDTO;
import com.labor.management.enums.ScoreType;
import com.labor.management.vo.ScoreSummaryVO;

import java.util.List;

/**
 * 成绩汇总 Service
 *
 * <p>查询所有学生的 4 项分数 + 最终总成绩，缺项标红支持手动补分。
 * 总成绩 = 考勤分×r1 + 课程报告×r2 + 理论学习×r3 + 项目实践×r4。
 * 考勤分由小程序端同步，归一化：各次课分数之和 / (10 × session_count) × 100。</p>
 */
public interface ScoreSummaryService {

    /**
     * 成绩汇总分页查询
     *
     * @param page     页码
     * @param size     每页条数
     * @param keyword  关键词（学号/姓名模糊）
     * @param companyId 公司ID（可选）
     * @param classId  班级ID（可选）
     * @param identity 身份筛选：null=全部, "STUDENT"=学生, "ASSISTANT"=助教
     * @param scopeClassIds 数据范围班级ID（null=不限制；非 null=限定 class_id IN 列表）
     * @return 分页结果（含 4 项分数 + finalScore + missingItems）
     */
    IPage<ScoreSummaryVO> getSummaryPage(Integer page, Integer size,
                                         String keyword, Long companyId, Long classId, String identity,
                                         List<Long> scopeClassIds);

    /**
     * 手动补分（单条新增/更新，复用导入的复活逻辑）
     *
     * @param type 成绩板块类型（COURSE_REPORT / THEORY / PRACTICE；ATTENDANCE 不走此接口）
     * @param dto  学生ID + 分数
     */
    void manualFill(ScoreType type, ScoreSingleSaveDTO dto);
}
