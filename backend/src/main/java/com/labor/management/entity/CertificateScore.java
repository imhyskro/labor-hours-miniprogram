package com.labor.management.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 换证考试成绩实体，对应 certificate_score 表。 */
@Data
@TableName("certificate_score")
public class CertificateScore implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long studentId;
    private Long classId;
    private String academicYear;
    private Integer semester;
    private BigDecimal finalScore;
    private String remark;
    private Long recordedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    @TableLogic
    private Integer deleted;
}
