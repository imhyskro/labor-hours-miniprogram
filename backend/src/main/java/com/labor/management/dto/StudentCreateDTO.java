package com.labor.management.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.io.Serializable;

/**
 * 学生新增 DTO
 */
@Data
public class StudentCreateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "学号不能为空")
    @Pattern(regexp = "\\d{9}", message = "学号必须是9位数字")
    private String studentId;

    @NotBlank(message = "姓名不能为空")
    private String name;

    @NotNull(message = "班级ID不能为空")
    private Long classId;

    /** 班级内编号 N（可选，完整编号 W-S-E-N 中的 N） */
    private Integer studentNoInClass;

    /** 原始专业（可选） */
    private String originalMajor;

    /** 性别: 1=男, 2=女 */
    private Integer gender;
}
