package com.labor.management.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 总表视图 VO
 *
 * <p>展示所有学生记录（普通学生 + 助教），层级为 公司 → 班级(周次-开始节次-结束节次) → 学生(班内编号)。
 * 助教负责的班级为多对多，聚合成文本展示。</p>
 */
@Data
public class MasterListViewVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** student 表主键 */
    private Long id;

    /** 学号 */
    private String studentId;

    /** 姓名 */
    private String name;

    /** 性别: 0=未知, 1=男, 2=女 */
    private Integer gender;

    /** 所属班级ID */
    private Long classId;

    /** 所属公司ID */
    private Long companyId;

    /** 所属公司名称 */
    private String companyName;

    /** 周次 W */
    private Integer week;

    /** 开始节次 S */
    private Integer startSession;

    /** 结束节次 E */
    private Integer endSession;

    /** 班级编码 W-S-E */
    private String classCode;

    /** 班级全名（公司-第W周-S~E节） */
    private String className;

    /** 班级内编号 N */
    private Integer studentNoInClass;

    /** 学生完整编号 W-S-E-N */
    private String fullNo;

    /** 原始专业 */
    private String originalMajor;

    /** 是否助教: 0=否, 1=是 */
    private Integer isAssistant;

    /** 身份文本: 学生 / 助教 */
    private String identity;

    /** 助教负责班级名称聚合（多对多，顿号分隔；无则 null） */
    private String assignedClassNames;

    /** 是否已建账号: 0=否, 1=是 */
    private Integer hasAccount;
}
