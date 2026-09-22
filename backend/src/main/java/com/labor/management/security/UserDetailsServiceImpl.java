package com.labor.management.security;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.labor.management.entity.SysRole;
import com.labor.management.entity.SysUser;
import com.labor.management.entity.UserRole;
import com.labor.management.mapper.SysRoleMapper;
import com.labor.management.mapper.SysUserMapper;
import com.labor.management.mapper.UserRoleMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Spring Security 用户加载服务
 *
 * <p>根据 username 查询 sys_user 表（含逻辑删除过滤 deleted=0），
 * 联查 user_role 和 sys_role 表获取角色列表。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final SysUserMapper sysUserMapper;
    private final UserRoleMapper userRoleMapper;
    private final SysRoleMapper sysRoleMapper;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // 查询用户（MyBatis-Plus @TableLogic 自动过滤 deleted=1）
        SysUser sysUser = sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getUsername, username)
        );
        if (sysUser == null) {
            throw new UsernameNotFoundException("用户不存在: " + username);
        }
        if (sysUser.getStatus() != null && sysUser.getStatus() == 0) {
            throw new UsernameNotFoundException("账号已被禁用: " + username);
        }

        // 查询角色编码列表
        List<String> roleCodes = loadRoleCodes(sysUser.getId());

        List<SimpleGrantedAuthority> authorities = roleCodes.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());

        Boolean firstLogin = sysUser.getFirstLogin() != null && sysUser.getFirstLogin() == 1;

        return new LoginUser(
                sysUser.getId(),
                sysUser.getUsername(),
                sysUser.getPasswordHash(),
                sysUser.getRealName(),
                firstLogin,
                authorities.isEmpty() ? Collections.emptyList() : authorities
        );
    }

    /**
     * 查询用户角色编码列表
     */
    public List<String> loadRoleCodes(Long userId) {
        List<UserRole> userRoles = userRoleMapper.selectList(
                new LambdaQueryWrapper<UserRole>()
                        .eq(UserRole::getUserId, userId)
        );
        if (userRoles.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> roleIds = userRoles.stream()
                .map(UserRole::getRoleId)
                .collect(Collectors.toList());
        List<SysRole> roles = sysRoleMapper.selectBatchIds(roleIds);
        return roles.stream()
                .map(SysRole::getRoleCode)
                .collect(Collectors.toList());
    }
}
