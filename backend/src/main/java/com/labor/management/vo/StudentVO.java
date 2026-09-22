package com.labor.management.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 学生响应 VO
 */
@Data
public class StudentVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String studentId;
    private String name;
    private Long classId;
    private String className;
    private Integer gender;
    private Integer status;
    private LocalDateTime createdAt;
}
