package com.labor.management.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 打分设置实体（单行表，固定 id=1）
 *
 * <p>对应数据库表 score_setting，保存上课次数（打分列数），教师可手动调整。</p>
 */
@Data
@TableName("score_setting")
public class ScoreSetting implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID（固定为1） */
    @TableId(type = IdType.INPUT)
    private Long id;

    /** 上课次数（打分列数） */
    private Integer sessionCount;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}
