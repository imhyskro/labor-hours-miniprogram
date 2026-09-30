package com.labor.management.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/** 提交课次成绩修改申请。 */
@Data
public class ModificationRequestCreateDTO implements Serializable {
    @NotNull(message = "课次ID不能为空")
    private Long sessionId;

    @NotNull(message = "学生ID不能为空")
    private Long studentId;

    @NotBlank(message = "申请原因不能为空")
    @Size(max = 500, message = "申请原因不能超过500个字符")
    private String reason;
}
