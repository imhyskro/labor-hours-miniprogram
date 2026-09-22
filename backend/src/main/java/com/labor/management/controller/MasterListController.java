package com.labor.management.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.labor.management.common.CommonResult;
import com.labor.management.service.DataScopeService;
import com.labor.management.service.MasterListService;
import com.labor.management.vo.MasterListViewVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 总表 Controller
 *
 * <p>所有 student 记录（普通学生 + 助教）的完整名单视图。
 * 权限：超级管理员 + 教师可访问；教师查询时自动应用数据隔离，仅返回其负责班级范围内的学生。</p>
 */
@RestController
@RequestMapping("/api/master-list")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('SUPER_ADMIN', 'TEACHER')")
public class MasterListController {

    private final MasterListService masterListService;
    private final DataScopeService dataScopeService;

    /**
     * 总表分页查询
     *
     * @param page     页码
     * @param size     每页条数
     * @param keyword  关键词（学号/姓名模糊）
     * @param classId  所属班级ID
     * @param identity 身份：STUDENT / ASSISTANT
     */
    @GetMapping
    public CommonResult<IPage<MasterListViewVO>> getMasterList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long companyId,
            @RequestParam(required = false) Long classId,
            @RequestParam(required = false) String identity) {
        return CommonResult.success(
                masterListService.getMasterList(page, size, keyword, companyId, classId, identity,
                        dataScopeService.getCurrentUserScopeClassIds())
        );
    }
}
