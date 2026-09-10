package com.labor.management.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 班级响应 VO
 */
@Data
public class ClassVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String className;
    /** 班级节次，格式：周次-开始节次-结束节次-班内编号，如1-1-2-9 */
    private String classCode;
    private String academicYear;
    private Integer status;
    private Long studentCount;
    private LocalDateTime createdAt;
}
