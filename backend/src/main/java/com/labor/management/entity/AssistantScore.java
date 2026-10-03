package com.labor.management.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 助教打分明细实体
 *
 * <p>对应数据库表 assistant_score。按 助教 × 第几次课 记录分数，每次课满分 10 分。</p>
 */
@Data
@TableName("assistant_score")
public class AssistantScore implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 助教（student 表主键） */
    private Long assistantStudentId;

    /** 打分对应的班级ID（assistant_class.class_id，标识这个助教在哪个班被打的分） */
    private Long classId;

    /** 第几次课（1=第一次课） */
    private Integer sessionNo;

    /** 本次课分数（0~10） */
    private BigDecimal score;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;

    /** 逻辑删除: 0=未删除, 1=已删除 */
    @TableLogic
    private Integer deleted;
}
