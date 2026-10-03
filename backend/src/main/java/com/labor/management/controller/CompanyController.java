package com.labor.management.controller;

import com.labor.management.common.CommonResult;
import com.labor.management.dto.CompanyCreateDTO;
import com.labor.management.service.CompanyService;
import com.labor.management.vo.CompanyStaffVO;
import com.labor.management.vo.CompanyVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 公司 Controller
 *
 * <p>班级管理的第一级页面展示公司列表；公司可增删、改名（加公司只需名字）。</p>
 * <p>权限：查询（listAll）超管+教师均可访问（教师单条新增学生/助教时需加载公司列表）；
 * 增删改仅超级管理员可操作。</p>
 *
 * <p>本控制器还提供按公司维度的「权限分配」增量覆盖接口：仅覆盖该公司范围内的
 * 教师/助教负责班级关联，不影响该人在其他公司的分配。</p>
 */
@RestController
@RequestMapping("/api/companies")
@RequiredArgsConstructor
@Tag(name = "公司管理", description = "公司的增删改查，以及按公司维度分配教师/助教负责班级")
public class CompanyController {

    private final CompanyService companyService;

    /** 查询所有公司（含班级数）：超管+教师均可 */
    @Operation(summary = "查询所有公司列表")
    @GetMapping
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN', 'TEACHER')")
    public CommonResult<List<CompanyVO>> listAll() {
        return CommonResult.success(companyService.listAll());
    }

    /**
     * 按公司聚合老师与助教名单（去重）
     *
     * <p>用于公司详情首行展示。返回未逻辑删除的老师账号与助教学生记录。</p>
     */
    @Operation(summary = "查询公司下的教师与助教名单")
    @GetMapping("/{companyId}/staff")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    public CommonResult<CompanyStaffVO> getCompanyStaff(@PathVariable Long companyId) {
        return CommonResult.success(companyService.getCompanyStaff(companyId));
    }

    /** 新增公司（只需名字） */
    @Operation(summary = "新增公司")
    @PostMapping
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    public CommonResult<Void> create(@Valid @RequestBody CompanyCreateDTO dto) {
        companyService.create(dto.getName());
        return CommonResult.success();
    }

    /** 公司改名 */
    @Operation(summary = "公司改名")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    public CommonResult<Void> rename(@PathVariable Long id,
                                     @Valid @RequestBody CompanyCreateDTO dto) {
        companyService.rename(id, dto.getName());
        return CommonResult.success();
    }

    /** 删除公司 */
    @Operation(summary = "删除公司")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    public CommonResult<Void> delete(@PathVariable Long id) {
        companyService.delete(id);
        return CommonResult.success();
    }

    /**
     * 按公司维度分配教师负责班级（增量覆盖）
     *
     * <p>请求体：{ "classIds": [1, 2, 3] }。仅覆盖该公司范围内的分配，
     * 不影响该教师在其他公司的分配。classIds 为空表示清空本公司的分配。</p>
     * <p>仅超级管理员可调用；后端会校验每个 classId 必须属于当前公司，否则返回 400。</p>
     */
    @Operation(summary = "按公司维度分配教师负责班级（增量覆盖）")
    @PutMapping("/{companyId}/teachers/{userId}/classes")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    public CommonResult<Void> assignTeacherClasses(@PathVariable Long companyId,
                                                   @PathVariable Long userId,
                                                   @RequestBody Map<String, Object> body) {
        companyService.assignTeacherClassesByCompany(companyId, userId, parseClassIds(body.get("classIds")));
        return CommonResult.success();
    }

    /**
     * 按公司维度分配助教负责班级（增量覆盖）
     *
     * <p>请求体：{ "classIds": [1, 2, 3] }。语义同 {@link #assignTeacherClasses}，
     * 作用于 assistant_class 关联表。</p>
     */
    @Operation(summary = "按公司维度分配助教负责班级（增量覆盖）")
    @PutMapping("/{companyId}/assistants/{studentId}/classes")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    public CommonResult<Void> assignAssistantClasses(@PathVariable Long companyId,
                                                     @PathVariable Long studentId,
                                                     @RequestBody Map<String, Object> body) {
        companyService.assignAssistantClassesByCompany(companyId, studentId, parseClassIds(body.get("classIds")));
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
