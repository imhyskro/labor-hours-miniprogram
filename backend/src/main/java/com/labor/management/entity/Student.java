package com.labor.management.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 学生实体
 *
 * <p>对应数据库表 student。学生是业务数据对象，非系统登录用户。
 * 助教通过 sys_user.student_id 关联学生。</p>
 */
@Data
@TableName("student")
public class Student implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 学号 */
    private String studentId;

    /** 学生姓名 */
    private String name;

    /** 所属班级ID（助教可能非本学期学生，允许为空） */
    private Long classId;

    /** 性别: 0=未知, 1=男, 2=女 */
    private Integer gender;

    /** 原始专业（学生本来的专业） */
    private String originalMajor;

    /** 班级内编号（完整编号 W-S-E-N 中的 N） */
    private Integer studentNoInClass;

    /** 状态: 1=在读, 0=停用 */
    private Integer status;

    /** 是否助教: 0=否, 1=是 */
    private Integer isAssistant;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;

    /** 逻辑删除: 0=未删除, 1=已删除 */
    @TableLogic
    private Integer deleted;
}
