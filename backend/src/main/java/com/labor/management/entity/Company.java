package com.labor.management.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 公司实体
 *
 * <p>对应数据库表 company。劳动课按公司（茶园、果园、待定…）划分，
 * 每个公司下按"周次-开始节次-结束节次"分为多个班级。</p>
 */
@Data
@TableName("company")
public class Company implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 公司名称 */
    private String name;

    /** 排序号 */
    private Integer sortOrder;

    /** 状态: 1=启用, 0=停用 */
    private Integer status;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;

    /** 逻辑删除: 0=未删除, 1=已删除 */
    @TableLogic
    private Integer deleted;
}
