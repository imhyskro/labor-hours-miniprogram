package com.labor.management.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.labor.management.common.CommonResult;
import com.labor.management.dto.StudentCreateDTO;
import com.labor.management.dto.StudentQueryDTO;
import com.labor.management.dto.StudentUpdateDTO;
import com.labor.management.service.DataScopeService;
import com.labor.management.service.StudentService;
import com.labor.management.vo.StudentVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 学生 Controller
 *
 * <p>权限：超级管理员 + 教师可访问；教师查询/操作时自动应用数据隔离，
 * 仅能查看/操作其负责班级范围内的学生。</p>
 */
@RestController
@RequestMapping("/api/students")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('SUPER_ADMIN', 'TEACHER')")
public class StudentController {

    private final StudentService studentService;
    private final DataScopeService dataScopeService;

    /** 分页查询学生列表（老师仅返回负责班级范围内的学生） */
    @GetMapping("/page")
    public CommonResult<IPage<StudentVO>> pageQuery(StudentQueryDTO queryDTO) {
        return CommonResult.success(studentService.pageQuery(queryDTO, dataScopeService.getCurrentUserScopeClassIds()));
    }

    /** 查询学生详情（老师仅可查自己负责班级内的学生） */
    @GetMapping("/{id}")
    public CommonResult<StudentVO> getById(@PathVariable Long id) {
        return CommonResult.success(studentService.getById(id, dataScopeService.getCurrentUserScopeClassIds()));
    }

    /** 新增学生（老师仅可向自己负责的班级新增） */
    @PostMapping
    public CommonResult<Void> create(@Valid @RequestBody StudentCreateDTO dto) {
        studentService.create(dto, dataScopeService.getCurrentUserScopeClassIds());
        return CommonResult.success();
    }

    /** 修改学生（老师仅可修改自己负责班级内的学生） */
    @PutMapping("/{id}")
    public CommonResult<Void> update(@PathVariable Long id,
                                     @Valid @RequestBody StudentUpdateDTO dto) {
        dto.setId(id);
        studentService.update(dto, dataScopeService.getCurrentUserScopeClassIds());
        return CommonResult.success();
    }

    /** 删除学生（逻辑删除；老师仅可删除自己负责班级内的学生） */
    @DeleteMapping("/{id}")
    public CommonResult<Void> delete(@PathVariable Long id) {
        studentService.deleteById(id, dataScopeService.getCurrentUserScopeClassIds());
        return CommonResult.success();
    }
}
