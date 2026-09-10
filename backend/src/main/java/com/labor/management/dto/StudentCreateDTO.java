package com.labor.management.dto;

import jakarta.validation.constraints.NotBlank;
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

    /** 性别: 1=男, 2=女 */
    private Integer gender;
}
