package com.labor.management.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 系统用户实体
 *
 * <p>对应数据库表 sys_user，存储教师/助教/管理员的登录账号信息。</p>
 */
@Data
@TableName("sys_user")
public class SysUser implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 登录用户名 */
    private String username;

    /** 密码哈希(BCrypt) */
    private String passwordHash;

    /** 真实姓名 */
    private String realName;

    /** 状态: 1=启用, 0=禁用 */
    private Integer status;

    /** 是否首次登录: 1=是, 0=否 */
    private Integer firstLogin;

    /** 最后密码修改时间 */
    private LocalDateTime lastPasswordChangeTime;

    /** 关联学生ID（助教账号关联学生记录主键） */
    private Long studentId;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;

    /** 逻辑删除: 0=未删除, 1=已删除 */
    @TableLogic
    private Integer deleted;
}
