package com.labor.management.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 成绩汇总视图 VO
 *
 * <p>在总表 {@link MasterListViewVO} 字段基础上增加 4 项分数 + 最终总成绩 + 缺项标识。
 * 总成绩 = 考勤分×r1 + 课程报告×r2 + 理论学习×r3 + 项目实践×r4（比例由 score_ratio_setting 表配置）。</p>
 * <p>考勤分由小程序端助教打分同步，归一化为百分制：各次课分数之和 / (10 × session_count) × 100。</p>
 * <p>缺项：任一项无分数 → finalScore 留空 + missingItems 列出缺项标识，前端标红支持手动补分。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ScoreSummaryVO extends MasterListViewVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 考勤分（百分制，归一化后；null=未同步） */
    private BigDecimal attendanceScore;

    /** 课程报告分数（0~100；null=未录入） */
    private BigDecimal courseReportScore;

    /** 理论学习分数（0~100；null=未录入） */
    private BigDecimal theoryScore;

    /** 项目实践分数（0~100；null=未录入） */
    private BigDecimal practiceScore;

    /** 最终总成绩（4 项加权；任一缺项则为 null） */
    private BigDecimal finalScore;

    /** 缺项标识列表（元素值：ATTENDANCE / COURSE_REPORT / THEORY / PRACTICE） */
    private List<String> missingItems;
}
