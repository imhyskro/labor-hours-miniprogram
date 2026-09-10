package com.labor.management.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 学生导入 DTO（Excel 单行解析结果）
 *
 * <p>字段顺序对应学生导入模板：学号 / 姓名 / 班级名称 / 性别。</p>
 */
@Data
public class StudentImportDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 学号 */
    private String studentId;

    /** 姓名 */
    private String name;

    /** 班级名称（按名称匹配 classes.class_name） */
    private String className;

    /** 性别: 1=男, 2=女, 0=未知；Excel 原始值为 "男"/"女"/"未知" 等，由 service 转换 */
    private String gender;
}
