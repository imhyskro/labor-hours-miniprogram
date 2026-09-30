package com.labor.management.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
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

    @DecimalMin(value = "0.00", message = "本次分数不能小于0")
    @DecimalMax(value = "10.00", message = "本次分数不能大于10")
    private BigDecimal score;

    @Size(max = 500, message = "备注不能超过500个字符")
    private String remark;

    /** 审批后修改时可显式指定申请；不传时查找当前用户可用的已通过申请。 */
    private Long changeRequestId;

    /** 修改原因，会写入不可变的 score_revision。 */
    @Size(max = 500, message = "修改原因不能超过500个字符")
    private String changeReason;
}
