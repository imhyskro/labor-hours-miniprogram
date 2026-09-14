package com.labor.management.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.labor.management.common.CommonResult;
import com.labor.management.service.AssistantAssignmentService;
import com.labor.management.vo.AssistantVO;
import com.labor.management.vo.MasterListViewVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 助教管理 Controller
 *
 * <p>老师/超级管理员可：查看助教列表、为助教分配多个负责班级（多对多）、
 * 设置/取消助教身份。助教本人不能访问本接口。</p>
 *
 * <p>权限注解使用 hasAnyAuthority('SUPER_ADMIN','TEACHER')，匹配权限存储方式
 * （authority 为纯字符串，无 ROLE_ 前缀）。</p>
 */
@RestController
@RequestMapping("/api/assistants")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('SUPER_ADMIN', 'TEACHER')")
public class AssistantController {

    private final AssistantAssignmentService assistantAssignmentService;

    /** 助教分页列表 */
    @GetMapping("/page")
    public CommonResult<IPage<AssistantVO>> pageAssistants(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String keyword) {
        return CommonResult.success(assistantAssignmentService.pageAssistants(page, size, keyword));
    }

    /** 查询助教当前负责的班级ID列表 */
    @GetMapping("/{studentId}/classes")
    public CommonResult<List<Long>> getAssistantClasses(@PathVariable Long studentId) {
        return CommonResult.success(assistantAssignmentService.getAssistantClassIds(studentId));
    }

    /**
     * 设置助教负责的班级（全量覆盖）
     * 请求体：{ "classIds": [1, 2, 3] }
     */
    @PutMapping("/{studentId}/classes")
    public CommonResult<Void> assignClasses(@PathVariable Long studentId,
                                            @RequestBody Map<String, Object> body) {
        Object obj = body.get("classIds");
        List<Long> classIds = new java.util.ArrayList<>();
        if (obj instanceof List<?> list) {
            for (Object o : list) {
                if (o instanceof Number n) {
                    classIds.add(n.longValue());
                } else if (o != null && !o.toString().isBlank()) {
                    classIds.add(Long.parseLong(o.toString().trim()));
                }
            }
        }
        assistantAssignmentService.assignClasses(studentId, classIds);
        return CommonResult.success();
    }

    /** 取消助教身份，恢复为普通学生（保留为兼容旧接口） */
    @PutMapping("/{studentId}/revoke")
    public CommonResult<Void> revoke(@PathVariable Long studentId) {
        assistantAssignmentService.revokeAssistant(studentId);
        return CommonResult.success();
    }

    /**
     * 设置或取消助教身份
     * <p>请求体：{ "isAssistant": true | false }</p>
     * <ul>
     *   <li>true：student.is_assistant=1，并创建登录账号（username=学号，
     *       初始密码 cdjcc123456，first_login=1 强制首登改密），关联 ASSISTANT 角色</li>
     *   <li>false：等价于取消助教（清理负责班级关联 + 删除登录账号 + is_assistant=0）</li>
     * </ul>
     */
    @PutMapping("/{studentId}/identity")
    public CommonResult<Void> setIdentity(@PathVariable Long studentId,
                                          @RequestBody Map<String, Object> body) {
        Object obj = body.get("isAssistant");
        boolean isAssistant;
        if (obj instanceof Boolean b) {
            isAssistant = b;
        } else if (obj instanceof String s) {
            isAssistant = "true".equalsIgnoreCase(s.trim());
        } else if (obj instanceof Number n) {
            isAssistant = n.intValue() != 0;
        } else {
            throw new com.labor.management.exception.BusinessException(
                    "请求体缺少 isAssistant 字段（应为 true/false）");
        }
        assistantAssignmentService.setIdentity(studentId, isAssistant);
        return CommonResult.success();
    }

    /** 查询尚未分配任何负责班级的助教列表 */
    @GetMapping("/unassigned")
    public CommonResult<List<MasterListViewVO>> getUnassignedAssistants() {
        return CommonResult.success(assistantAssignmentService.getUnassignedAssistants());
    }
}
