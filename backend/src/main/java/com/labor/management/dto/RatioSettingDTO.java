package com.labor.management.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 成绩比例设置 DTO
 *
 * <p>4 个比例之和必须 = 1（容差 0.0001）。
 * 总成绩 = 考勤分×attendanceRatio + 课程报告×courseReportRatio
 *        + 理论学习×theoryRatio + 项目实践×practiceRatio。</p>
 */
@Data
public class RatioSettingDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 考勤分比例 */
    private BigDecimal attendanceRatio;

    /** 课程报告比例 */
    private BigDecimal courseReportRatio;

    /** 理论学习比例 */
    private BigDecimal theoryRatio;

    /** 项目实践比例 */
    private BigDecimal practiceRatio;
}
