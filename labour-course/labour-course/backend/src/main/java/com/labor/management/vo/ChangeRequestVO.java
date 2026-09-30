package com.labor.management.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** V2 修改申请及其课次、学生、当前成绩上下文。 */
@Data
public class ChangeRequestVO implements Serializable {
    private Long id;
    private String requestNo;
    private String requestType;
    private String status;
    private String reason;
    private Long applicantUserId;
    private String applicantName;
    private Long scoreRecordId;
    private Long sessionId;
    private LocalDate sessionDate;
    private Long teachingGroupId;
    private String companyName;
    private String classCode;
    private Long studentId;
    private String studentNo;
    private String studentName;
    private BigDecimal currentScore;
    private String currentScoreMark;
    private LocalDateTime approvedAt;
    private LocalDateTime editWindowExpiresAt;
    private LocalDateTime usedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<ApprovalActionVO> actions;
}
