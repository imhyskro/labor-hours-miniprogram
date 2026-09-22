package com.labor.management.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.labor.management.common.CommonResult;
import com.labor.management.dto.ChangePasswordDTO;
import com.labor.management.dto.LoginDTO;
import com.labor.management.entity.SysUser;
import com.labor.management.exception.BusinessException;
import com.labor.management.mapper.SysUserMapper;
import com.labor.management.security.LoginUser;
import com.labor.management.security.UserDetailsServiceImpl;
import com.labor.management.util.JwtUtil;
import com.labor.management.util.PasswordValidator;
import com.labor.management.vo.LoginVO;
import com.labor.management.vo.UserInfoVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 认证 Controller
 *
 * <p>提供登录、修改密码接口。</p>
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final SysUserMapper sysUserMapper;
    private final UserDetailsServiceImpl userDetailsService;
    private final PasswordEncoder passwordEncoder;

    /**
     * 登录
     */
    @PostMapping("/login")
    public CommonResult<LoginVO> login(@Valid @RequestBody LoginDTO loginDTO) {
        // 使用 AuthenticationManager 校验用户名密码
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginDTO.getUsername(), loginDTO.getPassword())
        );

        User principal = (User) authentication.getPrincipal();
        String username = principal.getUsername();

        // 查询 sys_user 获取 first_login 字段
        SysUser sysUser = sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getUsername, username)
        );
        if (sysUser == null) {
            throw new BusinessException("用户不存在");
        }

        // 查询角色编码列表
        List<String> roleCodes = userDetailsService.loadRoleCodes(sysUser.getId());

        // 生成 JWT Token
        String token = jwtUtil.generateToken(username);

        // 构造返回结果
        UserInfoVO userInfo = new UserInfoVO();
        userInfo.setId(sysUser.getId());
        userInfo.setUsername(sysUser.getUsername());
        userInfo.setRealName(sysUser.getRealName());
        userInfo.setRoles(roleCodes);

        LoginVO loginVO = new LoginVO();
        loginVO.setToken(token);
        loginVO.setUserInfo(userInfo);
        loginVO.setFirstLogin(sysUser.getFirstLogin() != null && sysUser.getFirstLogin() == 1);

        log.info("用户登录成功: username={}, firstLogin={}", username, loginVO.getFirstLogin());
        return CommonResult.success(loginVO);
    }

    /**
     * 修改密码
     */
    @PostMapping("/change-password")
    @org.springframework.security.access.prepost.PreAuthorize("isAuthenticated()")
    public CommonResult<Void> changePassword(@Valid @RequestBody ChangePasswordDTO dto) {
        // 从 SecurityContext 获取当前用户名
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BusinessException("未登录");
        }
        String username = authentication.getName();

        // 查询用户
        SysUser sysUser = sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getUsername, username)
        );
        if (sysUser == null) {
            throw new BusinessException("用户不存在");
        }

        // 校验旧密码
        if (!passwordEncoder.matches(dto.getOldPassword(), sysUser.getPasswordHash())) {
            throw new BusinessException("旧密码不正确");
        }

        // 校验新密码与确认密码一致
        if (!dto.getNewPassword().equals(dto.getConfirmPassword())) {
            throw new BusinessException("新密码与确认密码不一致");
        }

        // 校验新密码复杂度
        PasswordValidator.validate(dto.getNewPassword());

        // 更新密码、first_login、last_password_change_time
        sysUser.setPasswordHash(passwordEncoder.encode(dto.getNewPassword()));
        sysUser.setFirstLogin(0);
        sysUser.setLastPasswordChangeTime(LocalDateTime.now());
        sysUserMapper.updateById(sysUser);

        log.info("用户修改密码成功: username={}", username);
        return CommonResult.success("密码修改成功", null);
    }
}
