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
    private Long scoreRecordId;
    private Long sessionId;
    private Long enrollmentId;
    private Long studentId;
    private String studentNo;
    private String studentName;
    private Integer studentNoInClass;
    /** 1=助教学生，不参与助教端打分及未打分统计。 */
    private Integer isAssistant;
    private String attendanceType;
    private BigDecimal score;
    private String scoreMark;
    private String remark;
    private Long recordedBy;
    private LocalDateTime updatedAt;
    private Integer revisionCount;
    private Boolean excludedFromScoring;
    private Boolean editable;
    private Boolean requiresApproval;
    private Long availableChangeRequestId;
    /** pending=待审批，approved=已通过待修改，其余为空。 */
    private String approvalStatus;
}
