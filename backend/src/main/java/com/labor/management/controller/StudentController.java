package com.labor.management.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.labor.management.common.CommonResult;
import com.labor.management.dto.StudentCreateDTO;
import com.labor.management.dto.StudentQueryDTO;
import com.labor.management.dto.StudentUpdateDTO;
import com.labor.management.service.StudentService;
import com.labor.management.vo.StudentVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 学生 Controller
 */
@RestController
@RequestMapping("/api/students")
@RequiredArgsConstructor
public class StudentController {

    private final StudentService studentService;

    /** 分页查询学生列表 */
    @GetMapping("/page")
    public CommonResult<IPage<StudentVO>> pageQuery(StudentQueryDTO queryDTO) {
        return CommonResult.success(studentService.pageQuery(queryDTO));
    }

    /** 查询学生详情 */
    @GetMapping("/{id}")
    public CommonResult<StudentVO> getById(@PathVariable Long id) {
        return CommonResult.success(studentService.getById(id));
    }

    /** 新增学生 */
    @PostMapping
    public CommonResult<Void> create(@Valid @RequestBody StudentCreateDTO dto) {
        studentService.create(dto);
        return CommonResult.success();
    }

    /** 修改学生 */
    @PutMapping("/{id}")
    public CommonResult<Void> update(@PathVariable Long id,
                                     @Valid @RequestBody StudentUpdateDTO dto) {
        dto.setId(id);
        studentService.update(dto);
        return CommonResult.success();
    }

    /** 删除学生（逻辑删除） */
    @DeleteMapping("/{id}")
    public CommonResult<Void> delete(@PathVariable Long id) {
        studentService.deleteById(id);
        return CommonResult.success();
    }
}
