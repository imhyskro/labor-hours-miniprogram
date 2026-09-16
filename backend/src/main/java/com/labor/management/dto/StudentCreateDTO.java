package com.labor.management.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * 学生新增 DTO
 */
@Data
public class StudentCreateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "学号不能为空")
    private String studentId;

    @NotBlank(message = "姓名不能为空")
    private String name;

    @NotNull(message = "班级ID不能为空")
    private Long classId;

    /** 班级内编号 N；同一班级内必须唯一。 */
    @NotNull(message = "班内编号不能为空")
    @Min(value = 1, message = "班内编号必须为正整数")
    private Integer studentNoInClass;

    /** 学生原始专业。 */
    private String originalMajor;

    /** 性别: 0=未知, 1=男, 2=女 */
    private Integer gender;
}
