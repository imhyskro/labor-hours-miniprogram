package com.labor.management.dto;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.time.LocalDateTime;

/** 操作日志分页查询条件。 */
@Data
public class OperationLogQueryDTO implements Serializable {

    private String moduleName;
    private String operationType;
    private Long operatorUserId;
    private String keyword;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime startTime;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime endTime;

    private Integer page = 1;
    private Integer size = 20;
}
