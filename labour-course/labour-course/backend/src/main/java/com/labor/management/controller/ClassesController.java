package com.labor.management.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.labor.management.common.CommonResult;
import com.labor.management.dto.ClassCreateDTO;
import com.labor.management.dto.ClassUpdateDTO;
import com.labor.management.service.ClassesService;
import com.labor.management.service.DataScopeService;
import com.labor.management.vo.ClassStudentVO;
import com.labor.management.vo.ClassVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 班级 Controller
 *
 * <p>权限：超级管理员 + 教师可访问；其中创建/修改/删除仅超级管理员可操作。
 * 教师查询时自动应用数据隔离，仅返回其负责班级范围内的数据。</p>
 */
@RestController
@RequestMapping("/api/classes")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('SUPER_ADMIN', 'TEACHER')")
public class ClassesController {

    private final ClassesService classesService;
    private final DataScopeService dataScopeService;

    /** 分页查询班级列表（老师仅返回负责班级） */
    @GetMapping("/page")
    public CommonResult<IPage<ClassVO>> pageQuery(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        return CommonResult.success(classesService.pageQuery(page, size, dataScopeService.getCurrentUserScopeClassIds()));
    }

    /** 查询所有班级（下拉/筛选用，含公司名与周次节次；老师仅返回负责班级） */
    @GetMapping("/list")
    public CommonResult<List<ClassVO>> listAll() {
        return CommonResult.success(classesService.listAll(dataScopeService.getCurrentUserScopeClassIds()));
    }

    /** 查询某公司下的所有班级（按周次-节次排序；老师仅返回负责班级） */
    @GetMapping("/company/{companyId}")
    public CommonResult<List<ClassVO>> listByCompany(@PathVariable Long companyId) {
        return CommonResult.success(classesService.listByCompany(companyId, dataScopeService.getCurrentUserScopeClassIds()));
    }

    /** 查询某班级的学生列表（按班级内编号排序；老师仅可查自己负责的班级） */
    @GetMapping("/{classId}/students")
    public CommonResult<List<ClassStudentVO>> getClassStudents(@PathVariable Long classId) {
        return CommonResult.success(classesService.getClassStudents(classId, dataScopeService.getCurrentUserScopeClassIds()));
    }

    /** 查询班级详情（老师仅可查自己负责的班级） */
    @GetMapping("/{id}")
    public CommonResult<ClassVO> getById(@PathVariable Long id) {
        return CommonResult.success(classesService.getById(id, dataScopeService.getCurrentUserScopeClassIds()));
    }

    /** 新增班级（仅超级管理员） */
    @PostMapping
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    public CommonResult<Void> create(@Valid @RequestBody ClassCreateDTO dto) {
        classesService.create(dto);
        return CommonResult.success();
    }

    /** 修改班级（仅超级管理员） */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    public CommonResult<Void> update(@PathVariable Long id,
                                     @Valid @RequestBody ClassUpdateDTO dto) {
        dto.setId(id);
        classesService.update(dto, dataScopeService.getCurrentUserScopeClassIds());
        return CommonResult.success();
    }

    /** 删除班级（逻辑删除；仅超级管理员） */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    public CommonResult<Void> delete(@PathVariable Long id) {
        classesService.deleteById(id, dataScopeService.getCurrentUserScopeClassIds());
        return CommonResult.success();
    }
}
