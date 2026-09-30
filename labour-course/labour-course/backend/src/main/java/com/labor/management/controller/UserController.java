package com.labor.management.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.labor.management.common.CommonResult;
import com.labor.management.dto.UserCreateDTO;
import com.labor.management.dto.UserQueryDTO;
import com.labor.management.service.UserService;
import com.labor.management.vo.ClassVO;
import com.labor.management.vo.UserVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 用户管理 Controller（仅超级管理员可访问）
 *
 * <p>接口前缀：/api/admin/users</p>
 * <p>权限：使用 hasAuthority 匹配权限存储方式（authority 为纯字符串 SUPER_ADMIN，
 * 无 ROLE_ 前缀，hasRole 会自动加前缀导致永远不生效）。</p>
 */
@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('SUPER_ADMIN')")
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

    /**
     * 创建教师账号
     * <p>入参：username + realName；密码用默认值 cdjcc123456，first_login=1（强制首登改密），
     * 自动绑定 TEACHER 角色。</p>
     *
     * @return 返回新创建的 userId
     */
    @PostMapping
    public CommonResult<Map<String, Long>> create(@Valid @RequestBody UserCreateDTO dto) {
        Long userId = userService.createTeacher(dto);
        return CommonResult.success(Map.of("userId", userId));
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

    /**
     * 查询教师负责的班级列表
     *
     * @param userId 教师 sys_user 主键
     */
    @GetMapping("/{userId}/classes")
    public CommonResult<List<ClassVO>> getTeacherClasses(@PathVariable Long userId) {
        return CommonResult.success(userService.getTeacherClasses(userId));
    }

    /**
     * 设置教师负责的班级（全量覆盖）
     * <p>请求体：{ "classIds": [1, 2, 3] }</p>
     *
     * @param userId 教师 sys_user 主键
     */
    @PutMapping("/{userId}/classes")
    public CommonResult<Void> assignTeacherClasses(@PathVariable Long userId,
                                                   @RequestBody Map<String, Object> body) {
        List<Long> classIds = parseClassIds(body.get("classIds"));
        userService.assignClassesToTeacher(userId, classIds);
        return CommonResult.success();
    }

    /**
     * 从请求体 classIds 字段解析 Long 列表，兼容 List<Number> / List<String>。
     */
    private List<Long> parseClassIds(Object obj) {
        List<Long> result = new java.util.ArrayList<>();
        if (obj instanceof List<?> list) {
            for (Object o : list) {
                if (o instanceof Number n) {
                    result.add(n.longValue());
                } else if (o != null && !o.toString().isBlank()) {
                    result.add(Long.parseLong(o.toString().trim()));
                }
            }
        }
        return result;
    }
}
