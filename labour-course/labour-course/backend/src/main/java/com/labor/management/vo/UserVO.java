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

    /** 角色编码列表 */
    private List<String> roles;

    /** 状态: 1=启用, 0=停用 */
    private Integer status;

    /** 是否首次登录: true=是 */
    private Boolean firstLogin;

    private LocalDateTime lastPasswordChangeTime;
    private LocalDateTime createdAt;
}
