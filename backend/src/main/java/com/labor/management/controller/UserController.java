package com.labor.management.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.labor.management.common.CommonResult;
import com.labor.management.dto.UserQueryDTO;
import com.labor.management.service.UserService;
import com.labor.management.vo.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 用户管理 Controller（仅超级管理员可访问）
 *
 * <p>接口前缀：/api/admin/users</p>
 */
@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class UserController {

    private final UserService userService;

    /** 分页查询用户列表（含角色、首次登录标志） */
    @GetMapping("/page")
    public CommonResult<IPage<UserVO>> pageQuery(UserQueryDTO queryDTO) {
        return CommonResult.success(userService.pageQuery(queryDTO));
    }

    /** 查询用户详情 */
    @GetMapping("/{userId}")
    public CommonResult<UserVO> getById(@PathVariable Long userId) {
        return CommonResult.success(userService.getById(userId));
    }

    /** 启用/停用用户 */
    @PutMapping("/{userId}/status")
    public CommonResult<Void> updateStatus(@PathVariable Long userId,
                                           @RequestParam Integer status) {
        userService.updateStatus(userId, status);
        return CommonResult.success();
    }

    /** 重置密码为默认值 cdjcc123456，first_login 置为 1 */
    @PutMapping("/{userId}/reset-password")
    public CommonResult<Void> resetPassword(@PathVariable Long userId) {
        userService.resetPassword(userId);
        return CommonResult.success();
    }
}
