package com.labor.management.controller;

import com.labor.management.common.CommonResult;
import com.labor.management.service.AssistantAssignmentService;
import com.labor.management.vo.MasterListViewVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 助教分配 Controller
 *
 * <p>分配/取消分配助教到班级；不限制班级唯一性（一个班级可分配多个助教）。
 * 所有接口暂不加 @PreAuthorize 权限控制。</p>
 */
@RestController
@RequestMapping("/api/assistants")
@RequiredArgsConstructor
public class AssistantController {

    private final AssistantAssignmentService assistantAssignmentService;

    /** 查询未分配（负责班级）的助教列表 */
    @GetMapping("/unassigned")
    public CommonResult<List<MasterListViewVO>> getUnassignedAssistants() {
        return CommonResult.success(assistantAssignmentService.getUnassignedAssistants());
    }

    /** 分配助教到班级（请求体：{ "classId": 1 }） */
    @PutMapping("/{studentId}/assign")
    public CommonResult<Void> assign(@PathVariable Long studentId,
                                     @RequestBody Map<String, Object> body) {
        Object classIdObj = body.get("classId");
        Long classId;
        if (classIdObj instanceof Number n) {
            classId = n.longValue();
        } else if (classIdObj instanceof String s && !s.isBlank()) {
            classId = Long.parseLong(s);
        } else {
            throw new IllegalArgumentException("classId 不能为空");
        }
        assistantAssignmentService.assignToClass(studentId, classId);
        return CommonResult.success();
    }

    /** 取消助教的班级分配 */
    @PutMapping("/{studentId}/unassign")
    public CommonResult<Void> unassign(@PathVariable Long studentId) {
        assistantAssignmentService.unassign(studentId);
        return CommonResult.success();
    }
}
