package com.labor.management.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** 劳动课课次响应。 */
@Data
public class AttendanceSessionVO implements Serializable {

    private Long id;
    private Long classId;
    private String className;
    private Integer weekNo;
    private LocalDate sessionDate;
    private Integer isLastSession;
    private Integer status;
    private Long createdBy;
    private Long updatedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
