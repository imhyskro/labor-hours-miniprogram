package com.labor.management.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.labor.management.common.ResultCode;
import com.labor.management.entity.AssistantClass;
import com.labor.management.entity.SysUser;
import com.labor.management.entity.TeacherClass;
import com.labor.management.exception.BusinessException;
import com.labor.management.mapper.AssistantClassMapper;
import com.labor.management.mapper.SysUserMapper;
import com.labor.management.mapper.TeacherClassMapper;
import com.labor.management.service.ClassAccessService;
import com.labor.management.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/** 考勤与成绩模块班级权限校验实现。 */
@Service
@RequiredArgsConstructor
public class ClassAccessServiceImpl implements ClassAccessService {

    private final TeacherClassMapper teacherClassMapper;
    private final AssistantClassMapper assistantClassMapper;
    private final SysUserMapper sysUserMapper;

    @Override
    public void checkTeacherAccess(Long classId) {
        if (classId == null) {
            throw new BusinessException("班级ID不能为空");
        }
        if (hasAuthority("SUPER_ADMIN")) {
            return;
        }
        Long userId = requireCurrentUserId();
        if (hasAuthority("TEACHER")) {
            Long count = teacherClassMapper.selectCount(
                    new LambdaQueryWrapper<TeacherClass>()
                            .eq(TeacherClass::getUserId, userId)
                            .eq(TeacherClass::getClassId, classId));
            if (count != null && count > 0) {
                return;
            }
        }
        throw new BusinessException(ResultCode.PERMISSION_DENIED);
    }

    @Override
    public void checkAttendanceAccess(Long classId) {
        if (hasAuthority("SUPER_ADMIN") || hasAuthority("TEACHER")) {
            checkTeacherAccess(classId);
            return;
        }
        if (classId == null) {
            throw new BusinessException("班级ID不能为空");
        }
        Long assistantStudentId = getCurrentAssistantStudentId();
        if (assistantStudentId != null) {
            Long count = assistantClassMapper.selectCount(
                    new LambdaQueryWrapper<AssistantClass>()
                            .eq(AssistantClass::getAssistantStudentId, assistantStudentId)
                            .eq(AssistantClass::getClassId, classId));
            if (count != null && count > 0) {
                return;
            }
        }
        throw new BusinessException(ResultCode.PERMISSION_DENIED);
    }

    @Override
    public Long getCurrentAssistantStudentId() {
        if (!hasAuthority("ASSISTANT")) {
            return null;
        }
        Long userId = requireCurrentUserId();
        SysUser user = sysUserMapper.selectById(userId);
        return user == null ? null : user.getStudentId();
    }

    private Long requireCurrentUserId() {
        Long userId = SecurityUtil.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        return userId;
    }

    private boolean hasAuthority(String role) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            if (role.equals(authority.getAuthority())) {
                return true;
            }
        }
        return false;
    }
}
