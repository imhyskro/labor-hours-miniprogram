package com.labor.management.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/** 新增或修改单个学生考勤记录的请求。 */
@Data
public class AttendanceRecordSaveDTO implements Serializable {

    /** NORMAL=正常，J=事假，K=旷课。 */
    @NotBlank(message = "考勤类型不能为空")
    @Pattern(regexp = "NORMAL|J|K", message = "考勤类型只能是 NORMAL、J 或 K")
    private String attendanceType;

    @NotNull(message = "本次分数不能为空")
    @DecimalMin(value = "0.00", message = "本次分数不能小于0")
    @DecimalMax(value = "10.00", message = "本次分数不能大于10")
    private BigDecimal score;

    @Size(max = 255, message = "备注不能超过255个字符")
    private String remark;
}
