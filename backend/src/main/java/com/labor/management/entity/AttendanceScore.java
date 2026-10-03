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
 * 考勤分明细实体
 *
 * <p>对应数据库表 attendance_score。按 学生 × 第几次课 记录分数，每次课满分 10 分。
 * 由小程序端助教打分同步写入；本次仅建表 + 成绩汇总读取，不实现写入接口。</p>
 * <p>唯一键 uk_student_session(student_id, session_no) 不含 deleted。</p>
 */
@Data
@TableName("attendance_score")
public class AttendanceScore implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 学生ID（关联 student.id） */
    private Long studentId;

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
