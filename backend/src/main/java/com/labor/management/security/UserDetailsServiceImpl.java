package com.labor.management.security;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

/** Loads Web users from the V2 account and role tables. */
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {
    private final JdbcTemplate jdbcTemplate;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        List<UserRow> users = jdbcTemplate.query("""
                SELECT id, username, password_hash, real_name, status, first_login
                FROM sys_user WHERE username = ?
                """, (rs, rowNum) -> new UserRow(rs.getLong("id"), rs.getString("username"),
                rs.getString("password_hash"), rs.getString("real_name"), rs.getString("status"),
                rs.getBoolean("first_login")), username);
        if (users.isEmpty()) throw new UsernameNotFoundException("用户不存在: " + username);
        UserRow user = users.get(0);
        if (!"ACTIVE".equals(user.status())) {
            throw new UsernameNotFoundException("账号已被禁用或锁定: " + username);
        }
        List<SimpleGrantedAuthority> authorities = loadRoleCodes(user.id()).stream()
                .map(SimpleGrantedAuthority::new).toList();
        return new LoginUser(user.id(), user.username(), user.passwordHash(), user.realName(),
                user.firstLogin(), authorities);
    }

    /** V2 stores ADMIN; keep the existing Web contract SUPER_ADMIN at the API boundary. */
    public List<String> loadRoleCodes(Long userId) {
        return jdbcTemplate.queryForList("""
                SELECT CASE WHEN r.role_code = 'ADMIN' THEN 'SUPER_ADMIN' ELSE r.role_code END
                FROM user_role ur JOIN sys_role r ON r.id = ur.role_id
                WHERE ur.user_id = ? ORDER BY r.id
                """, String.class, userId);
    }

    public UserAccount findAccount(String username) {
        List<UserAccount> users = jdbcTemplate.query("""
                SELECT id, username, password_hash, real_name, first_login
                FROM sys_user WHERE username = ?
                """, (rs, rowNum) -> new UserAccount(rs.getLong("id"), rs.getString("username"),
                rs.getString("password_hash"), rs.getString("real_name"), rs.getBoolean("first_login")), username);
        return users.isEmpty() ? null : users.get(0);
    }

    public record UserAccount(Long id, String username, String passwordHash,
                              String realName, boolean firstLogin) {}
    private record UserRow(Long id, String username, String passwordHash, String realName,
                           String status, boolean firstLogin) {}
}
