package com.labor.management.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

/**
 * 班级新增 DTO
 */
@Data
public class ClassCreateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "班级名称不能为空")
    private String className;

    @NotBlank(message = "班级节次不能为空")
    private String classCode;

    @NotBlank(message = "学年不能为空")
    private String academicYear;
}
