package com.labor.management.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 总表视图 VO
 *
 * <p>展示所有学生记录（普通学生 + 助教），包含所属班级名称、身份、负责班级、是否已建账号。</p>
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

    /** 所属班级名称 */
    private String className;

    /** 是否助教: 0=否, 1=是 */
    private Integer isAssistant;

    /** 身份文本: 学生 / 助教 */
    private String identity;

    /** 助教负责班级ID */
    private Long assignedClassId;

    /** 助教负责班级名称（无则 null） */
    private String assignedClassName;

    /** 是否已建账号: 0=否, 1=是 */
    private Integer hasAccount;
}
