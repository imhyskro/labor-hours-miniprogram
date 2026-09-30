package com.labor.management.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;

/** 修改劳动课课次请求；本接口仅管理员和教师可调用。 */
@Data
public class AttendanceSessionUpdateDTO implements Serializable {

    @NotNull(message = "周次不能为空")
    @Min(value = 1, message = "周次必须为正整数")
    private Integer weekNo;

    private LocalDate sessionDate;
    private Integer isLastSession;

    /** 1=可编辑，0=封存。 */
    private Integer status;
}
