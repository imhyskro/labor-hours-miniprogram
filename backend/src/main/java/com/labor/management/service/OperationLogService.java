package com.labor.management.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.labor.management.dto.OperationLogQueryDTO;
import com.labor.management.vo.OperationLogVO;

/** 操作日志记录与查询服务。 */
public interface OperationLogService {

    IPage<OperationLogVO> pageQuery(OperationLogQueryDTO query);

    void record(String moduleName, String operationType, String targetType, Long targetId,
                String description, Object beforeData, Object afterData);
}
