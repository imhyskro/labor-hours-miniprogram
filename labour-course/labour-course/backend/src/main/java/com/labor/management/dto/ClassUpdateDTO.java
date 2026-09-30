package com.labor.management.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 班级修改 DTO
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class ClassUpdateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    @NotNull(message = "周次不能为空")
    private Integer week;

    @NotNull(message = "开始节次不能为空")
    private Integer startSession;

    @NotNull(message = "结束节次不能为空")
    private Integer endSession;

    private Integer status;
}
