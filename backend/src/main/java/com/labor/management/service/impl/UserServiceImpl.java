package com.labor.management.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.labor.management.dto.UserQueryDTO;
import com.labor.management.entity.SysUser;
import com.labor.management.exception.BusinessException;
import com.labor.management.mapper.SysUserMapper;
import com.labor.management.security.UserDetailsServiceImpl;
import com.labor.management.service.UserService;
import com.labor.management.util.SecurityUtil;
import com.labor.management.vo.UserVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

/**
 * 用户 Service 实现（仅超级管理员可操作）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final SysUserMapper sysUserMapper;
    private final UserDetailsServiceImpl userDetailsService;
    private final PasswordEncoder passwordEncoder;

    /** 初始默认密码，从配置读取（便于统一调整） */
    @Value("${app.default-password:cdjcc123456}")
    private String defaultPassword;

    @Override
    public IPage<UserVO> pageQuery(UserQueryDTO queryDTO) {
        Page<SysUser> pageParam = new Page<>(queryDTO.getPage(), queryDTO.getSize());
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<SysUser>()
                .orderByDesc(SysUser::getCreatedAt);

        if (StringUtils.hasText(queryDTO.getKeyword())) {
            String kw = queryDTO.getKeyword().trim();
            wrapper.and(w -> w.like(SysUser::getUsername, kw)
                    .or().like(SysUser::getRealName, kw));
        }
        if (queryDTO.getStatus() != null) {
            wrapper.eq(SysUser::getStatus, queryDTO.getStatus());
        }

        IPage<SysUser> entityPage = sysUserMapper.selectPage(pageParam, wrapper);

        Page<UserVO> resultPage = new Page<>(entityPage.getCurrent(), entityPage.getSize(), entityPage.getTotal());
        resultPage.setRecords(entityPage.getRecords().stream()
                .map(this::toVO)
                .collect(Collectors.toList()));
        return resultPage;
    }

    @Override
    public UserVO getById(Long id) {
        SysUser entity = sysUserMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException("用户不存在");
        }
        return toVO(entity);
    }

    @Override
    public void updateStatus(Long userId, Integer status) {
        Long currentUserId = SecurityUtil.getCurrentUserId();
        // 校验不能操作自己
        if (currentUserId != null && currentUserId.equals(userId)) {
            throw new BusinessException("不能停用或重置自己的账号");
        }

        SysUser existing = sysUserMapper.selectById(userId);
        if (existing == null) {
            throw new BusinessException("用户不存在");
        }
        existing.setStatus(status);
        existing.setUpdatedAt(LocalDateTime.now());
        sysUserMapper.updateById(existing);
        log.info("超级管理员修改用户状态: targetUserId={}, status={}", userId, status);
    }

    @Override
    public void resetPassword(Long userId) {
        Long currentUserId = SecurityUtil.getCurrentUserId();
        // 校验不能操作自己
        if (currentUserId != null && currentUserId.equals(userId)) {
            throw new BusinessException("不能停用或重置自己的账号");
        }

        SysUser existing = sysUserMapper.selectById(userId);
        if (existing == null) {
            throw new BusinessException("用户不存在");
        }
        existing.setPasswordHash(passwordEncoder.encode(defaultPassword));
        existing.setFirstLogin(1);
        existing.setLastPasswordChangeTime(LocalDateTime.now());
        existing.setUpdatedAt(LocalDateTime.now());
        sysUserMapper.updateById(existing);
        log.info("超级管理员重置用户密码: targetUserId={}", userId);
    }

    /**
     * 实体转 VO，补充角色列表和 firstLogin 布尔值
     */
    private UserVO toVO(SysUser entity) {
        UserVO vo = new UserVO();
        BeanUtils.copyProperties(entity, vo);
        vo.setRoles(userDetailsService.loadRoleCodes(entity.getId()));
        vo.setFirstLogin(entity.getFirstLogin() != null && entity.getFirstLogin() == 1);
        return vo;
    }
}
