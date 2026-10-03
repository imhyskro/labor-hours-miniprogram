package com.labor.management.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户响应 VO（含角色列表）
 *
 * <p>区别于登录用的 UserInfoVO，本类用于用户管理页面展示。</p>
 */
@Data
public class UserVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String username;
    private String realName;

    /**
     * 关联的学生主键 ID（仅助教账号有值；通过 username=学号 关联到 student 表）
     *
     * <p>用于用户管理页"取消助教"操作：调用 /api/assistants/{studentId}/revoke 接口。</p>
     */
    private Long studentId;

    /** 角色编码列表 */
    private List<String> roles;

    /** 状态: 1=启用, 0=停用 */
    private Integer status;

    /** 是否首次登录: true=是 */
    private Boolean firstLogin;

    private LocalDateTime lastPasswordChangeTime;
    private LocalDateTime createdAt;
}
