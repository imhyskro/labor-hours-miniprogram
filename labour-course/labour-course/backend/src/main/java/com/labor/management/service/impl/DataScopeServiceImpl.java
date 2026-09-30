package com.labor.management.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.labor.management.entity.TeacherClass;
import com.labor.management.mapper.TeacherClassMapper;
import com.labor.management.security.LoginUser;
import com.labor.management.service.DataScopeService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 数据范围服务实现
 */
@Service
@RequiredArgsConstructor
public class DataScopeServiceImpl implements DataScopeService {

    private final TeacherClassMapper teacherClassMapper;

    @Override
    public boolean isCurrentUserSuperAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }
        for (GrantedAuthority ga : auth.getAuthorities()) {
            if ("SUPER_ADMIN".equals(ga.getAuthority())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public List<Long> getCurrentUserScopeClassIds() {
        // 超管不限制
        if (isCurrentUserSuperAdmin()) {
            return null;
        }
        // 老师：查 teacher_class 关联
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof LoginUser loginUser)) {
            return List.of();
        }
        List<TeacherClass> rels = teacherClassMapper.selectList(
                new LambdaQueryWrapper<TeacherClass>().eq(TeacherClass::getUserId, loginUser.getId())
        );
        return rels.stream()
                .map(TeacherClass::getClassId)
                .distinct()
                .collect(Collectors.toList());
    }
}
