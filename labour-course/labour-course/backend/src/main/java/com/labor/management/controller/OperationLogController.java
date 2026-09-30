package com.labor.management.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.labor.management.common.CommonResult;
import com.labor.management.dto.OperationLogQueryDTO;
import com.labor.management.service.OperationLogService;
import com.labor.management.vo.OperationLogVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 系统操作日志查询接口。 */
@RestController
@RequestMapping("/api/operation-logs")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('SUPER_ADMIN')")
public class OperationLogController {

    private final OperationLogService operationLogService;

    /** 分页查询操作日志，可按模块、类型、操作人、关键词和时间范围筛选。 */
    @GetMapping
    public CommonResult<IPage<OperationLogVO>> pageQuery(OperationLogQueryDTO query) {
        return CommonResult.success(operationLogService.pageQuery(query));
    }
}
