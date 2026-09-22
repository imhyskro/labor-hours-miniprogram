package com.labor.management.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** 劳动课周次实体，对应 attendance_session 表。 */
@Data
@TableName("attendance_session")
public class AttendanceSession implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long classId;
    private Integer weekNo;
    private LocalDate sessionDate;
    private Integer isLastSession;
    private Integer status;
    private Long createdBy;
    private Long updatedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    @TableLogic
    private Integer deleted;
}
