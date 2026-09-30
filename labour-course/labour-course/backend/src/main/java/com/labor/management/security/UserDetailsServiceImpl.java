package com.labor.management.security;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

/** 从 V2 sys_user、user_role、sys_role 加载认证主体。 */
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final JdbcTemplate jdbc;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Account account = findAccount(username);
        if (!"ACTIVE".equals(account.status())) {
            throw new UsernameNotFoundException("账号不可用: " + username);
        }
        List<SimpleGrantedAuthority> authorities = loadRoleCodes(account.id()).stream()
                .map(SimpleGrantedAuthority::new)
                .toList();
        return new LoginUser(account.id(), account.username(), account.passwordHash(),
                account.realName(), account.firstLogin(), authorities);
    }

    public List<String> loadRoleCodes(Long userId) {
        return jdbc.queryForList("""
                SELECT r.role_code
                  FROM user_role ur
                  JOIN sys_role r ON r.id=ur.role_id
                 WHERE ur.user_id=?
                 ORDER BY r.id
                """, String.class, userId);
    }

    private Account findAccount(String username) {
        try {
            return jdbc.queryForObject("""
                    SELECT id, username, password_hash, real_name, status, first_login
                      FROM sys_user WHERE username=?
                    """, (rs, rowNum) -> new Account(
                    rs.getLong("id"), rs.getString("username"),
                    rs.getString("password_hash"), rs.getString("real_name"),
                    rs.getString("status"), rs.getBoolean("first_login")), username);
        } catch (EmptyResultDataAccessException ex) {
            throw new UsernameNotFoundException("用户不存在: " + username);
        }
    }

    private record Account(Long id, String username, String passwordHash,
                           String realName, String status, boolean firstLogin) {
    }
}
