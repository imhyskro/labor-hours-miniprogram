package com.labor.management.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/** 一键打分请求：为指定学生批量设置相同的考勤类型与分数。 */
@Data
public class AttendanceBatchScoreDTO implements Serializable {

    /** 待打分学生 ID 列表。 */
    @NotEmpty(message = "学生列表不能为空")
    private List<Long> studentIds;

    /** 统一分数（0~10）。 */
    @NotNull(message = "本次分数不能为空")
    @DecimalMin(value = "0.00", message = "本次分数不能小于0")
    @DecimalMax(value = "10.00", message = "本次分数不能大于10")
    private BigDecimal score;

    /** 考勤类型，默认 NORMAL。 */
    private String attendanceType = "NORMAL";

    /** 备注，默认空。 */
    private String remark = "";
}
