package com.labor.management.security;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.Collection;

/**
 * 自定义登录用户主体
 *
 * <p>继承 Spring Security 的 User，额外携带用户 ID、真实姓名、首次登录标志，
 * 方便在 Service 层通过 SecurityContextHolder 直接获取当前用户信息。</p>
 */
@Getter
public class LoginUser extends User {

    private final Long id;
    private final String realName;
    private final Boolean firstLogin;

    public LoginUser(Long id,
                     String username,
                     String password,
                     String realName,
                     Boolean firstLogin,
                     Collection<? extends GrantedAuthority> authorities) {
        super(username, password, true, true, true, true, authorities);
        this.id = id;
        this.realName = realName;
        this.firstLogin = firstLogin;
    }
}
