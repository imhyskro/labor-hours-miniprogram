package com.labor.management.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * 学生修改 DTO
 */
@Data
public class StudentUpdateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "ID不能为空")
    private Long id;

    @NotBlank(message = "学号不能为空")
    private String studentId;

    @NotBlank(message = "姓名不能为空")
    private String name;

    /** 所属班级ID；本学期未选课时允许为空 */
    private Long classId;

    /** 班级内编号 N（可选） */
    private Integer studentNoInClass;

    /** 原始专业（可选） */
    private String originalMajor;

    /** 性别: 1=男, 2=女 */
    private Integer gender;

    /** 状态: 1=在读, 0=停用 */
    @NotNull(message = "状态不能为空")
    private Integer status;
}
