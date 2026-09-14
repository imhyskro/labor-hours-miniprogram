package com.labor.management.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 班级响应 VO
 *
 * <p>班级 = 某公司在"第W周 第S~E节"的一次劳动安排。</p>
 */
@Data
public class ClassVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    /** 班级名称（自动生成，如"茶园-第1周-1~2节"） */
    private String className;

    /** 班级编码：周次-开始节次-结束节次，如 1-1-2 */
    private String classCode;

    /** 所属公司ID */
    private Long companyId;

    /** 所属公司名称 */
    private String companyName;

    /** 周次 */
    private Integer week;

    /** 开始节次 */
    private Integer startSession;

    /** 结束节次 */
    private Integer endSession;

    private String academicYear;
    private Integer status;

    /** 班级学生数 */
    private Long studentCount;

    private LocalDateTime createdAt;
}
