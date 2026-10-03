package com.labor.management.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.labor.management.common.CommonResult;
import com.labor.management.dto.ScoreSingleSaveDTO;
import com.labor.management.enums.ScoreType;
import com.labor.management.service.DataScopeService;
import com.labor.management.service.ScoreSummaryService;
import com.labor.management.vo.ScoreSummaryVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 成绩汇总 Controller
 *
 * <p>权限：仅超级管理员可访问。查询所有学生 4 项分数 + 最终总成绩，缺项标红支持手动补分。</p>
 */
@RestController
@RequestMapping("/api/scores/summary")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('SUPER_ADMIN')")
@Tag(name = "成绩汇总", description = "查询所有学生四项分数与总成绩，支持缺项手动补分（仅超级管理员）")
public class ScoreSummaryController {

    private final ScoreSummaryService scoreSummaryService;
    private final DataScopeService dataScopeService;

    /**
     * 成绩汇总分页查询
     */
    @Operation(summary = "成绩汇总分页查询")
    @GetMapping
    public CommonResult<IPage<ScoreSummaryVO>> getSummary(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long companyId,
            @RequestParam(required = false) Long classId,
            @RequestParam(required = false) String identity) {
        return CommonResult.success(scoreSummaryService.getSummaryPage(
                page, size, keyword, companyId, classId, identity,
                dataScopeService.getCurrentUserScopeClassIds()
        ));
    }

    /**
     * 手动补分（单条新增/更新）
     *
     * <p>请求体含 type（COURSE_REPORT / THEORY / PRACTICE）+ studentId + score</p>
     */
    @Operation(summary = "手动补分（单条新增/更新）")
    @PostMapping("/manual")
    public CommonResult<Void> manualFill(@RequestBody Map<String, Object> body) {
        String typeStr = (String) body.get("type");
        ScoreType type = ScoreType.valueOf(typeStr);
        ScoreSingleSaveDTO dto = new ScoreSingleSaveDTO();
        Object studentIdObj = body.get("studentId");
        if (studentIdObj instanceof Number) {
            dto.setStudentId(((Number) studentIdObj).longValue());
        } else {
            dto.setStudentId(Long.valueOf(body.get("studentId").toString()));
        }
        Object scoreObj = body.get("score");
        if (scoreObj instanceof Number) {
            dto.setScore(new java.math.BigDecimal(scoreObj.toString()));
        } else {
            dto.setScore(new java.math.BigDecimal(body.get("score").toString()));
        }
        scoreSummaryService.manualFill(type, dto);
        return CommonResult.success();
    }
}
