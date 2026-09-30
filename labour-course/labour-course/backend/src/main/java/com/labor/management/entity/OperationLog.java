package com.labor.management.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/** 系统操作日志实体，对应 operation_log 表。 */
@Data
@TableName("operation_log")
public class OperationLog implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long operatorUserId;
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
