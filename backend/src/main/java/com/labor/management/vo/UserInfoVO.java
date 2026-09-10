package com.labor.management.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 登录后返回的用户信息
 */
@Data
public class UserInfoVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 用户ID */
    private Long id;

    /** 用户名 */
    private String username;

    /** 真实姓名 */
    private String realName;

    /** 角色编码列表 */
    private List<String> roles;
}
