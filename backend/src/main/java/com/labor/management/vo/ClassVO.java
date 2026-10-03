package com.labor.management.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 班级响应 VO
 *
 * <p>班级 = 某公司在"第W周几 第S~E节"的一次劳动安排。</p>
 * <p>字段语义：W = 周几（1=周一…5=周五，非周次），S = 开始节次，E = 结束节次。</p>
 */
@Data
public class ClassVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    /** 班级名称（自动生成，如"茶园-周一-1~2节"） */
    private String className;

    /** 班级节次编码：周几-开始节次-结束节次，如 1-1-2 */
    private String classCode;

    /** 所属公司ID */
    private Long companyId;

    /** 所属公司名称 */
    private String companyName;

    /** 周几（1=周一…5=周五，非周次） */
    private Integer week;

    /** 开始节次 */
    private Integer startSession;

    /** 结束节次 */
    private Integer endSession;

    private String academicYear;
    private Integer status;

    /** 班级学生数 */
    private Long studentCount;

    /** 已分配负责老师数（teacher_class 关联行数） */
    private Long teacherCount;

    /** 已分配负责助教数（assistant_class 关联行数） */
    private Long assistantCount;

    private LocalDateTime createdAt;
}
