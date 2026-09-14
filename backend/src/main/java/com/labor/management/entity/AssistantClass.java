package com.labor.management.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 助教负责班级关联实体（多对多）
 *
 * <p>对应数据库表 assistant_class。一个助教可负责多个班级，
 * 一个班级也可分配多个助教。</p>
 */
@Data
@TableName("assistant_class")
public class AssistantClass implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 助教（student 表主键） */
    private Long assistantStudentId;

    /** 负责班级ID */
    private Long classId;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;

    /** 逻辑删除: 0=未删除, 1=已删除 */
    @TableLogic
    private Integer deleted;
}
