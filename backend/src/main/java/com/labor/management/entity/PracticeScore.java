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
 * 项目实践分数实体
 *
 * <p>对应数据库表 practice_score。每个学生一个总分（0~100）。
 * 唯一键 uk_student(student_id) 不含 deleted，导入时需复活已删记录。</p>
 */
@Data
@TableName("practice_score")
public class PracticeScore implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 学生ID（关联 student.id） */
    private Long studentId;

    /** 分数（0~100） */
    private BigDecimal score;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;

    /** 逻辑删除: 0=未删除, 1=已删除 */
    @TableLogic
    private Integer deleted;
}
