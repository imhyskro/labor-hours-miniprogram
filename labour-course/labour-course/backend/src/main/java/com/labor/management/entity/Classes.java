package com.labor.management.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 班级实体
 *
 * <p>对应数据库表 classes。班级 = 某公司在"第W周 第S~E节"的一次劳动安排，
 * 班级编码格式为"周次-开始节次-结束节次"（如 1-1-2）；
 * 学生在班内另有编号 N，完整编号为 W-S-E-N。</p>
 */
@Data
@TableName("classes")
public class Classes implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 班级名称（自动生成，如"茶园-第1周-1~2节"） */
    private String className;

    /** 班级编码，格式：周次-开始节次-结束节次，如1-1-2 */
    private String classCode;

    /** 所属公司ID */
    private Long companyId;

    /** 周次（第几周） */
    private Integer week;

    /** 开始节次 */
    private Integer startSession;

    /** 结束节次 */
    private Integer endSession;

    /** 学年, 如 2024-2025 */
    private String academicYear;

    /** 状态: 1=启用, 0=归档 */
    private Integer status;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;

    /** 逻辑删除: 0=未删除, 1=已删除 */
    @TableLogic
    private Integer deleted;
}
