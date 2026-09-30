package com.labor.management.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/** 审批动作流水。 */
@Data
public class ApprovalActionVO implements Serializable {
    private Long id;
    private String action;
    private Long operatorUserId;
    private String operatorName;
    private String comment;
    private LocalDateTime actionAt;
}
