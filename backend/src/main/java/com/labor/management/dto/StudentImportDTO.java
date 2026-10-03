package com.labor.management.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 学生导入 DTO（Excel 单行解析结果）
 *
 * <p>列顺序：学号 / 姓名 / 公司名称 / 周次 / 开始节次 / 结束节次 / 班内编号 / 原始专业 / 性别。
 * 数字字段在 Excel 中以字符串读取，由 Service 转换。</p>
 */
@Data
public class StudentImportDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 学号 */
    private String studentId;
    /** 姓名 */
    private String name;
    /** 公司名称 */
    private String companyName;
    /** 周次（字符串，Service 转 Integer） */
    private String week;
    /** 开始节次 */
    private String startSession;
    /** 结束节次 */
    private String endSession;
    /** 班级内编号 */
    private String studentNoInClass;
    /** 原始专业 */
    private String originalMajor;
    /** 性别: 男/女 */
    private String gender;
}
