package com.labor.management.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 单条成绩保存 DTO
 *
 * <p>用于单条新增 / 手动补分（成绩汇总页缺项单元格点击编辑后调用）。
 * studentId 为 student 表主键 ID（非学号字符串）。</p>
 */
@Data
public class ScoreSingleSaveDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 学生ID（student.id） */
    private Long studentId;

    /** 分数（0~100） */
    private BigDecimal score;
}
