package com.labor.management.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 班级课次考勤记录响应。
 *
 * <p>即使某学生尚未登记，也会返回学生信息，此时 id、attendanceType、score 等记录字段为 null。</p>
 */
@Data
public class AttendanceRecordVO implements Serializable {

    private Long id;
    private Long sessionId;
    private Long studentId;
    private String studentNo;
    private String studentName;
    private Integer studentNoInClass;
    private String attendanceType;
    private BigDecimal score;
    private String remark;
    private Long recordedBy;
    private LocalDateTime updatedAt;
}
