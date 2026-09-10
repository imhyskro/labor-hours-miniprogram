package com.labor.management.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 导入错误行 VO
 */
@Data
public class ImportErrorVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Excel 行号（数据行从 2 开始，1 为表头） */
    private Integer row;

    /** 学号（便于定位） */
    private String studentId;

    /** 失败原因 */
    private String reason;

    public ImportErrorVO() {
    }

    public ImportErrorVO(Integer row, String studentId, String reason) {
        this.row = row;
        this.studentId = studentId;
        this.reason = reason;
    }
}
