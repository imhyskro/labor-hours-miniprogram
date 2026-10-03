package com.labor.management.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 班级内学生 VO（按班级内编号排序展示）
 */
@Data
public class ClassStudentVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    /** 学号 */
    private String studentId;

    /** 姓名 */
    private String name;

    /** 班级内编号 N */
    private Integer studentNoInClass;

    /** 完整编号 W-S-E-N */
    private String fullNo;

    /** 原始专业 */
    private String originalMajor;

    /** 性别: 0=未知, 1=男, 2=女 */
    private Integer gender;

    /** 是否助教: 0=否, 1=是 */
    private Integer isAssistant;
}
