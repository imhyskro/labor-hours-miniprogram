package com.labor.management.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 成绩导入 Excel 行映射 DTO
 *
 * <p>对应 Excel 4 列：学号 / 姓名 / 班级 / 分数。
 * 用于课程报告 / 理论学习 / 项目实践 三个板块的批量导入。</p>
 */
@Data
public class ScoreImportDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 学号 */
    private String studentId;

    /** 姓名（用于与系统记录核对） */
    private String name;

    /** 班级名称（用于与系统记录核对） */
    private String className;

    /** 分数（0~100） */
    private BigDecimal score;
}
