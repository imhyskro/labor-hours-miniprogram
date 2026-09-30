package com.labor.management.controller;

import com.labor.management.common.CommonResult;
import com.labor.management.common.ResultCode;
import com.labor.management.dto.ChangePasswordDTO;
import com.labor.management.dto.LoginDTO;
import com.labor.management.exception.BusinessException;
import com.labor.management.security.UserDetailsServiceImpl;
import com.labor.management.util.JwtUtil;
import com.labor.management.util.PasswordValidator;
import com.labor.management.vo.LoginVO;
import com.labor.management.vo.UserInfoVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
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

import java.util.List;

/** V2 账号登录和密码修改接口。 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final JdbcTemplate jdbc;
    private final UserDetailsServiceImpl userDetailsService;
    private final PasswordEncoder passwordEncoder;

    @PostMapping("/login")
    public CommonResult<LoginVO> login(@Valid @RequestBody LoginDTO loginDTO) {
        return loginInternal(loginDTO, false);
    }

    /** 小程序只允许单一助教身份，教师和管理员只能从Web端登录。 */
    @PostMapping("/mini-login")
    public CommonResult<LoginVO> miniLogin(@Valid @RequestBody LoginDTO loginDTO) {
        return loginInternal(loginDTO, true);
    }

    private CommonResult<LoginVO> loginInternal(LoginDTO dto, boolean assistantOnly) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(dto.getUsername(), dto.getPassword()));
        String username = ((User) authentication.getPrincipal()).getUsername();
        Account account = findAccount(username);
        List<String> roles = userDetailsService.loadRoleCodes(account.id());
        if (assistantOnly && (!roles.contains("ASSISTANT")
                || roles.contains("ADMIN") || roles.contains("SUPER_ADMIN") || roles.contains("TEACHER"))) {
            throw new BusinessException(ResultCode.PERMISSION_DENIED, "微信小程序仅允许助教账号登录");
        }

        jdbc.update("UPDATE sys_user SET last_login_at=CURRENT_TIMESTAMP(3) WHERE id=?", account.id());
        UserInfoVO userInfo = new UserInfoVO();
        userInfo.setId(account.id());
        userInfo.setUsername(account.username());
        userInfo.setRealName(account.realName());
        userInfo.setRoles(roles);

        LoginVO vo = new LoginVO();
        vo.setToken(jwtUtil.generateToken(username));
        vo.setUserInfo(userInfo);
        vo.setFirstLogin(account.firstLogin());
        log.info("用户登录成功: username={}, client={}", username,
                assistantOnly ? "mini-program" : "web");
        return CommonResult.success(vo);
    }

    @PostMapping("/change-password")
    @org.springframework.security.access.prepost.PreAuthorize("isAuthenticated()")
    public CommonResult<Void> changePassword(@Valid @RequestBody ChangePasswordDTO dto) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        Account account = findAccount(authentication.getName());
        if (!passwordEncoder.matches(dto.getOldPassword(), account.passwordHash())) {
            throw new BusinessException("旧密码不正确");
        }
        if (!dto.getNewPassword().equals(dto.getConfirmPassword())) {
            throw new BusinessException("新密码与确认密码不一致");
        }
        PasswordValidator.validate(dto.getNewPassword());
        jdbc.update("""
                UPDATE sys_user
                   SET password_hash=?, first_login=FALSE,
                       last_password_change_at=CURRENT_TIMESTAMP(3)
                 WHERE id=?
                """, passwordEncoder.encode(dto.getNewPassword()), account.id());
        return CommonResult.success("密码修改成功", null);
    }

    private Account findAccount(String username) {
        try {
            return jdbc.queryForObject("""
                    SELECT id, username, password_hash, real_name, first_login
                      FROM sys_user WHERE username=?
                    """, (rs, rowNum) -> new Account(rs.getLong("id"),
                    rs.getString("username"), rs.getString("password_hash"),
                    rs.getString("real_name"), rs.getBoolean("first_login")), username);
        } catch (EmptyResultDataAccessException ex) {
            throw new BusinessException("用户不存在");
        }
    }

    private record Account(Long id, String username, String passwordHash,
                           String realName, boolean firstLogin) {
    }
}
