package com.labor.management.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 学生单次劳动课考勤与分数实体，对应 attendance_record 表。 */
@Data
@TableName("attendance_record")
public class AttendanceRecord implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long sessionId;
    private Long studentId;
    private String attendanceType;
    private BigDecimal score;
    private String remark;
    private Long recordedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    @TableLogic
    private Integer deleted;
}
