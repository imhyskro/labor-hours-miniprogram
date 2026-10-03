package com.labor.management.controller;

import com.labor.management.common.CommonResult;
import com.labor.management.dto.ChangePasswordDTO;
import com.labor.management.dto.LoginDTO;
import com.labor.management.exception.BusinessException;
import com.labor.management.security.UserDetailsServiceImpl;
import com.labor.management.util.JwtUtil;
import com.labor.management.util.PasswordValidator;
import com.labor.management.vo.LoginVO;
import com.labor.management.vo.UserInfoVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "认证管理", description = "提供用户登录认证与登录后修改密码接口")
public class AuthController {
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final UserDetailsServiceImpl userDetailsService;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbcTemplate;

    @Operation(summary = "用户登录")
    @PostMapping("/login")
    public CommonResult<LoginVO> login(@Valid @RequestBody LoginDTO loginDTO) {
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
                loginDTO.getUsername(), loginDTO.getPassword()));
        UserDetailsServiceImpl.UserAccount user = userDetailsService.findAccount(loginDTO.getUsername());
        if (user == null) throw new BusinessException("用户不存在");
        List<String> roles = userDetailsService.loadRoleCodes(user.id());
        jdbcTemplate.update("UPDATE sys_user SET last_login_at = CURRENT_TIMESTAMP(3) WHERE id = ?", user.id());
        UserInfoVO info = new UserInfoVO();
        info.setId(user.id());
        info.setUsername(user.username());
        info.setRealName(user.realName());
        info.setRoles(roles);
        LoginVO result = new LoginVO();
        result.setToken(jwtUtil.generateToken(user.username()));
        result.setUserInfo(info);
        result.setFirstLogin(user.firstLogin());
        log.info("用户登录成功: username={}, firstLogin={}", user.username(), user.firstLogin());
        return CommonResult.success(result);
    }

    @Operation(summary = "修改密码")
    @PostMapping("/change-password")
    @org.springframework.security.access.prepost.PreAuthorize("isAuthenticated()")
    public CommonResult<Void> changePassword(@Valid @RequestBody ChangePasswordDTO dto) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) throw new BusinessException("未登录");
        UserDetailsServiceImpl.UserAccount user = userDetailsService.findAccount(authentication.getName());
        if (user == null) throw new BusinessException("用户不存在");
        if (!passwordEncoder.matches(dto.getOldPassword(), user.passwordHash())) throw new BusinessException("旧密码不正确");
        if (!dto.getNewPassword().equals(dto.getConfirmPassword())) throw new BusinessException("新密码与确认密码不一致");
        PasswordValidator.validate(dto.getNewPassword());
        jdbcTemplate.update("""
                UPDATE sys_user SET password_hash = ?, first_login = FALSE,
                    last_password_change_at = CURRENT_TIMESTAMP(3) WHERE id = ?
                """, passwordEncoder.encode(dto.getNewPassword()), user.id());
        return CommonResult.success("密码修改成功", null);
    }
}
