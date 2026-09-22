package com.labor.management.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 学生导入 DTO（Excel 单行解析结果）
 *
 * <p>列顺序：公司 / 节次 / 学号 / 姓名 / 性别 / 行政班。
 * 节次格式为 W-S-E-N（周次-开始节次-结束节次-班内编号），由 Service 拆分。</p>
 */
@Data
public class StudentImportDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 公司 */
    private String companyName;
    /** 节次编码：周次-开始节次-结束节次-班内编号 */
    private String sessionCode;
    /** 学号 */
    private String studentId;
    /** 姓名 */
    private String name;
    /** 性别: 男/女 */
    private String gender;
    /** 行政班（当前保存到 student.originalMajor） */
    private String administrativeClass;
}
