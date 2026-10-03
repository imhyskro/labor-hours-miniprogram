package com.labor.management.service.impl;

import com.labor.management.security.LoginUser;
import com.labor.management.service.DataScopeService;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DataScopeServiceImpl implements DataScopeService {
    private final JdbcTemplate jdbcTemplate;

    @Override
    public boolean isCurrentUserSuperAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return false;
        return auth.getAuthorities().stream().map(GrantedAuthority::getAuthority)
                .anyMatch(role -> "SUPER_ADMIN".equals(role) || "ADMIN".equals(role));
    }

    @Override
    public List<Long> getCurrentUserScopeClassIds() {
        if (isCurrentUserSuperAdmin()) return null;
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof LoginUser user)) {
            return List.of();
        }
        return jdbcTemplate.queryForList("""
                SELECT id FROM teaching_group
                WHERE current_teacher_user_id = ? AND status <> 'ARCHIVED'
                ORDER BY id
                """, Long.class, user.getId());
    }
}
