package com.labor.management.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.labor.management.common.CommonResult;
import com.labor.management.dto.ClassCreateDTO;
import com.labor.management.dto.ClassUpdateDTO;
import com.labor.management.service.ClassesService;
import com.labor.management.vo.ClassVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 班级 Controller
 */
@RestController
@RequestMapping("/api/classes")
@RequiredArgsConstructor
public class ClassesController {

    private final ClassesService classesService;

    /** 分页查询班级列表 */
    @GetMapping("/page")
    public CommonResult<IPage<ClassVO>> pageQuery(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        return CommonResult.success(classesService.pageQuery(page, size));
    }

    /** 查询所有启用的班级（下拉框用） */
    @GetMapping("/list")
    public CommonResult<List<ClassVO>> listAll() {
        return CommonResult.success(classesService.listAll());
    }

    /** 查询班级详情 */
    @GetMapping("/{id}")
    public CommonResult<ClassVO> getById(@PathVariable Long id) {
        return CommonResult.success(classesService.getById(id));
    }

    /** 新增班级 */
    @PostMapping
    public CommonResult<Void> create(@Valid @RequestBody ClassCreateDTO dto) {
        classesService.create(dto);
        return CommonResult.success();
    }

    /** 修改班级 */
    @PutMapping("/{id}")
    public CommonResult<Void> update(@PathVariable Long id,
                                     @Valid @RequestBody ClassUpdateDTO dto) {
        dto.setId(id);
        classesService.update(dto);
        return CommonResult.success();
    }

    /** 删除班级（逻辑删除） */
    @DeleteMapping("/{id}")
    public CommonResult<Void> delete(@PathVariable Long id) {
        classesService.deleteById(id);
        return CommonResult.success();
    }
}
