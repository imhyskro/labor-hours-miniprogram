package com.labor.management.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/** 新增或修改换证考试成绩请求。 */
@Data
public class CertificateScoreSaveDTO implements Serializable {

    @NotNull(message = "班级ID不能为空")
    private Long classId;

    @NotBlank(message = "学年不能为空")
    private String academicYear;

    @NotNull(message = "学期不能为空")
    @Min(value = 1, message = "学期只能是1或2")
    @Max(value = 2, message = "学期只能是1或2")
    private Integer semester;

    @NotNull(message = "最终成绩不能为空")
    @DecimalMin(value = "0.00", message = "最终成绩不能小于0")
    @DecimalMax(value = "100.00", message = "最终成绩不能大于100")
    private BigDecimal finalScore;

    @Size(max = 255, message = "备注不能超过255个字符")
    private String remark;
}
