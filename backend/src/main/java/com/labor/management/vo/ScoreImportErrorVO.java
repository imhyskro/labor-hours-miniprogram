package com.labor.management.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 成绩导入错误行 VO（增强版，明确指出预期值 vs 实际值）
 *
 * <p>每行错误包含：行号 / 学号（Excel 填的）/ 错误类型 / 详细原因 / 预期值 / 实际值，
 * 便于人工核查对账。错误类型枚举见类常量 ERROR_TYPE_*。</p>
 */
@Data
public class ScoreImportErrorVO implements Serializable {

    private static final long serialVersionUID = 1L;

    // ====== 错误类型常量 ======
    /** 学号不存在 */
    public static final String ERROR_TYPE_STUDENT_NOT_FOUND = "学号不存在";
    /** 姓名不匹配 */
    public static final String ERROR_TYPE_NAME_MISMATCH = "姓名不匹配";
    /** 班级不匹配 */
    public static final String ERROR_TYPE_CLASS_MISMATCH = "班级不匹配";
    /** 班级不存在 */
    public static final String ERROR_TYPE_CLASS_NOT_FOUND = "班级不存在";
    /** 分数非法 */
    public static final String ERROR_TYPE_SCORE_INVALID = "分数非法";
    /** 学号重复 */
    public static final String ERROR_TYPE_DUPLICATE = "学号重复";
    /** 班级不齐全 */
    public static final String ERROR_TYPE_CLASS_INCOMPLETE = "班级不齐全";
    /** 字段为空 */
    public static final String ERROR_TYPE_FIELD_EMPTY = "字段为空";

    /** Excel 行号（数据行从 2 开始，1 为表头） */
    private Integer row;

    /** 学号（Excel 填的，便于定位） */
    private String studentId;

    /** 错误类型（见 ERROR_TYPE_* 常量） */
    private String errorType;

    /** 详细原因（人类可读，含预期值 vs 实际值） */
    private String reason;

    /** 预期值（系统记录的值） */
    private String expectedValue;

    /** 实际值（Excel 填的值） */
    private String actualValue;

    public ScoreImportErrorVO() {
    }

    public ScoreImportErrorVO(Integer row, String studentId, String errorType,
                              String reason, String expectedValue, String actualValue) {
        this.row = row;
        this.studentId = studentId;
        this.errorType = errorType;
        this.reason = reason;
        this.expectedValue = expectedValue;
        this.actualValue = actualValue;
    }
}
