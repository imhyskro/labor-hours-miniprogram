package com.labor.management.controller;

import com.labor.management.common.CommonResult;
import com.labor.management.dto.ScoreSaveDTO;
import com.labor.management.service.AssistantScoreService;
import com.labor.management.service.DataScopeService;
import com.labor.management.vo.AssistantScoreTableVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 助教打分 Controller
 *
 * <p>权限：超级管理员 + 教师可访问；教师仅能查看/操作其负责班级范围内的助教。</p>
 */
@RestController
@RequestMapping("/api/assistant-scores")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('SUPER_ADMIN', 'TEACHER')")
@Tag(name = "助教打分", description = "教师对助教按课次打分，含打分表查询、分数保存与上课次数调整")
public class AssistantScoreController {

    private final AssistantScoreService assistantScoreService;
    private final DataScopeService dataScopeService;

    /**
     * 查询打分表（上课次数 + 当前页助教行及分数）
     */
    @Operation(summary = "查询助教打分表")
    @GetMapping("/table")
    public CommonResult<AssistantScoreTableVO> getScoreTable(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String keyword) {
        return CommonResult.success(assistantScoreService.getScoreTable(
                page, size, keyword, dataScopeService.getCurrentUserScopeClassIds()));
    }

    /**
     * 批量保存分数（教师仅可给其负责班级内的助教打分）
     */
    @Operation(summary = "批量保存助教分数")
    @PostMapping("/scores")
    public CommonResult<Void> saveScores(@RequestBody ScoreSaveDTO dto) {
        assistantScoreService.saveScores(dto, dataScopeService.getCurrentUserScopeClassIds());
        return CommonResult.success();
    }

    /**
     * 调整上课次数（分数列数；仅超级管理员，影响所有用户的全局设置）
     */
    @Operation(summary = "调整上课次数（仅超级管理员）")
    @PutMapping("/session-count")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    public CommonResult<Void> updateSessionCount(@RequestParam Integer count) {
        assistantScoreService.updateSessionCount(count);
        return CommonResult.success();
    }
}
