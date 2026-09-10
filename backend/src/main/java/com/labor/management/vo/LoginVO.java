package com.labor.management.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 登录返回结果
 */
@Data
public class LoginVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** JWT Token */
    private String token;

    /** 用户信息 */
    private UserInfoVO userInfo;

    /** 是否首次登录（需强制修改密码） */
    private Boolean firstLogin;
}
