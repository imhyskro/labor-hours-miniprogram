package com.labor.management.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 成绩比例设置实体（单行表，固定 id=1）
 *
 * <p>对应数据库表 score_ratio_setting。4 个比例之和必须 = 1。
 * 总成绩 = 考勤分×attendanceRatio + 课程报告×courseReportRatio
 *        + 理论学习×theoryRatio + 项目实践×practiceRatio。</p>
 */
@Data
@TableName("score_ratio_setting")
public class ScoreRatioSetting implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID（固定为 1） */
    @TableId(type = IdType.INPUT)
    private Long id;

    /** 考勤分比例 */
    private BigDecimal attendanceRatio;

    /** 课程报告比例 */
    private BigDecimal courseReportRatio;

    /** 理论学习比例 */
    private BigDecimal theoryRatio;

    /** 项目实践比例 */
    private BigDecimal practiceRatio;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}
