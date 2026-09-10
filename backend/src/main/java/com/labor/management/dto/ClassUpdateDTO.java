package com.labor.management.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * 班级修改 DTO
 */
@Data
public class ClassUpdateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "ID不能为空")
    private Long id;

    @NotBlank(message = "班级名称不能为空")
    private String className;

    @NotBlank(message = "班级节次不能为空")
    private String classCode;

    @NotBlank(message = "学年不能为空")
    private String academicYear;

    /** 状态: 1=启用, 0=停用 */
    @NotNull(message = "状态不能为空")
    private Integer status;
}
