package com.labor.management.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;

/** 新建劳动课课次请求。 */
@Data
public class AttendanceSessionCreateDTO implements Serializable {

    @NotNull(message = "班级ID不能为空")
    private Long classId;

    @NotNull(message = "周次不能为空")
    @Min(value = 1, message = "周次必须为正整数")
    private Integer weekNo;

    private LocalDate sessionDate;

    /** 是否最后一次课：0=否，1=是。 */
    private Integer isLastSession = 0;
}
