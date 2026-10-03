package com.labor.management.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 助教管理 VO
 *
 * <p>助教 = is_assistant=1 的学生；老师可为助教分配多个负责班级（assistant_class 多对多）。</p>
 */
@Data
public class AssistantVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** student 表主键 */
    private Long id;

    /** 学号（登录账号） */
    private String studentId;

    /** 姓名 */
    private String name;

    /** 性别: 0=未知, 1=男, 2=女 */
    private Integer gender;

    /** 原始专业 */
    private String originalMajor;

    /** 所属公司名称 */
    private String companyName;

    /** 所属班级编码 W-S-E */
    private String classCode;

    /** 所属班级全名 */
    private String className;

    /** 班级内编号 */
    private Integer studentNoInClass;

    /** 完整编号 W-S-E-N */
    private String fullNo;

    /** 是否已建登录账号: 0=否, 1=是 */
    private Integer hasAccount;

    /** 负责班级数量 */
    private Long assignedClassCount;

    /** 负责班级名称聚合（顿号分隔） */
    private String assignedClassNames;
}
