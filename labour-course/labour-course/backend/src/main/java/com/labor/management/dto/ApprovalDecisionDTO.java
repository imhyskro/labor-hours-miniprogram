package com.labor.management.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/** 教师或管理员审批修改申请。 */
@Data
public class ApprovalDecisionDTO implements Serializable {
    @NotBlank(message = "审批决定不能为空")
    @Pattern(regexp = "APPROVE|REJECT", message = "审批决定只能是 APPROVE 或 REJECT")
    private String decision;

    @Size(max = 500, message = "审批意见不能超过500个字符")
    private String comment;

    /** 通过后允许修改的小时数；不传时读取 business_config。 */
    @Min(value = 1, message = "修改窗口不能小于1小时")
    @Max(value = 168, message = "修改窗口不能超过168小时")
    private Integer editWindowHours;
}
