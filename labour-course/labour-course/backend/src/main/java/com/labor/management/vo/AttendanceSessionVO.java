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
    private String weekLabel;
    private LocalDate sessionDate;
    private Integer isLastSession;
    private Integer status;
    private String statusCode;
    private Long createdBy;
    private Long updatedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /** 该课次未打分学生数（班级学生总数 - 已有考勤记录的学生数）。0=已全部打分。 */
    private Integer unscoredCount;

    /** 当前日期是否处于可打分窗口。 */
    private Boolean editable;

    /** 不可打分时的原因；可编辑时为“可编辑”。 */
    private String lockReason;

    /** 自动封存日期（课次日期 + sealDays）。 */
    private LocalDate sealDate;

    /** 当前生效的自动封存天数配置。 */
    private Integer sealDays;

    /** V2 课次固化的成绩截止时间。 */
    private LocalDateTime scoreDeadlineAt;
}
