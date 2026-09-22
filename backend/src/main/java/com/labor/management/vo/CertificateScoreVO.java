package com.labor.management.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 班级换证考试成绩响应。
 *
 * <p>尚未录入成绩的学生也会返回，此时 id 和 finalScore 等成绩字段为 null。</p>
 */
@Data
public class CertificateScoreVO implements Serializable {

    private Long id;
    private Long studentId;
    private String studentNo;
    private String studentName;
    private Integer studentNoInClass;
    private Long classId;
    private String academicYear;
    private Integer semester;
    private BigDecimal finalScore;
    private String remark;
    private Long recordedBy;
    private LocalDateTime updatedAt;
}
