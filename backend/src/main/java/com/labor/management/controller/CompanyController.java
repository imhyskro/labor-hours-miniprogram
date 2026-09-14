package com.labor.management.controller;

import com.labor.management.common.CommonResult;
import com.labor.management.dto.CompanyCreateDTO;
import com.labor.management.service.CompanyService;
import com.labor.management.vo.CompanyVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 公司 Controller
 *
 * <p>班级管理的第一级页面展示公司列表；公司可增删、改名（加公司只需名字）。</p>
 * <p>权限：仅超级管理员可访问（公司属基础配置数据，老师/助教不应管理）。</p>
 */
@RestController
@RequestMapping("/api/companies")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('SUPER_ADMIN')")
public class CompanyController {

    private final CompanyService companyService;

    /** 查询所有公司（含班级数） */
    @GetMapping
    public CommonResult<List<CompanyVO>> listAll() {
        return CommonResult.success(companyService.listAll());
    }

    /** 新增公司（只需名字） */
    @PostMapping
    public CommonResult<Void> create(@Valid @RequestBody CompanyCreateDTO dto) {
        companyService.create(dto.getName());
        return CommonResult.success();
    }

    /** 公司改名 */
    @PutMapping("/{id}")
    public CommonResult<Void> rename(@PathVariable Long id,
                                     @Valid @RequestBody CompanyCreateDTO dto) {
        companyService.rename(id, dto.getName());
        return CommonResult.success();
    }

    /** 删除公司 */
    @DeleteMapping("/{id}")
    public CommonResult<Void> delete(@PathVariable Long id) {
        companyService.delete(id);
        return CommonResult.success();
    }
}
