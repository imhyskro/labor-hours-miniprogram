package com.labor.management.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/** 操作日志查询响应。 */
@Data
public class OperationLogVO implements Serializable {

    private Long id;
    private Long operatorUserId;
    private String operatorUsername;
    private String operatorName;
    private String moduleName;
    private String operationType;
    private String targetType;
    private Long targetId;
    private String description;
    private String beforeData;
    private String afterData;
    private String clientIp;
    private LocalDateTime createdAt;
}
