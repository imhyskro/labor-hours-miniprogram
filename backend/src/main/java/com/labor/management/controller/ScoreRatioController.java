package com.labor.management.controller;

import com.labor.management.common.CommonResult;
import com.labor.management.dto.RatioSettingDTO;
import com.labor.management.entity.ScoreRatioSetting;
import com.labor.management.service.ScoreRatioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 成绩比例设置 Controller
 *
 * <p>权限：仅超级管理员可访问。4 个比例之和必须 = 1。</p>
 */
@RestController
@RequestMapping("/api/scores/ratio")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('SUPER_ADMIN')")
@Tag(name = "成绩比例设置", description = "四项成绩占比的读取与更新，比例之和必须等于1（仅超级管理员）")
public class ScoreRatioController {

    private final ScoreRatioService scoreRatioService;

    /** 读取比例设置 */
    @Operation(summary = "读取成绩比例设置")
    @GetMapping
    public CommonResult<ScoreRatioSetting> getRatio() {
        return CommonResult.success(scoreRatioService.getRatio());
    }

    /** 更新比例设置 */
    @Operation(summary = "更新成绩比例设置")
    @PutMapping
    public CommonResult<Void> updateRatio(@RequestBody RatioSettingDTO dto) {
        scoreRatioService.updateRatio(dto);
        return CommonResult.success();
    }
}
