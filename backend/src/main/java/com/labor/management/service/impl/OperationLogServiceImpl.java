package com.labor.management.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.labor.management.dto.OperationLogQueryDTO;
import com.labor.management.entity.OperationLog;
import com.labor.management.entity.SysUser;
import com.labor.management.exception.BusinessException;
import com.labor.management.mapper.OperationLogMapper;
import com.labor.management.mapper.SysUserMapper;
import com.labor.management.service.OperationLogService;
import com.labor.management.util.SecurityUtil;
import com.labor.management.vo.OperationLogVO;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/** 操作日志记录与查询服务实现。 */
@Service
@RequiredArgsConstructor
public class OperationLogServiceImpl implements OperationLogService {

    private final OperationLogMapper operationLogMapper;
    private final SysUserMapper sysUserMapper;
    private final ObjectMapper objectMapper;

    @Override
    public IPage<OperationLogVO> pageQuery(OperationLogQueryDTO query) {
        long pageNo = query.getPage() == null || query.getPage() < 1 ? 1 : query.getPage();
        long pageSize = query.getSize() == null || query.getSize() < 1 ? 20 : query.getSize();
        pageSize = Math.min(pageSize, 200);

        LambdaQueryWrapper<OperationLog> wrapper = new LambdaQueryWrapper<OperationLog>()
                .eq(StringUtils.hasText(query.getModuleName()), OperationLog::getModuleName,
                        trimToNull(query.getModuleName()))
                .eq(StringUtils.hasText(query.getOperationType()), OperationLog::getOperationType,
                        trimToNull(query.getOperationType()))
                .eq(query.getOperatorUserId() != null, OperationLog::getOperatorUserId,
                        query.getOperatorUserId())
                .ge(query.getStartTime() != null, OperationLog::getCreatedAt, query.getStartTime())
                .le(query.getEndTime() != null, OperationLog::getCreatedAt, query.getEndTime())
                .and(StringUtils.hasText(query.getKeyword()), nested -> nested
                        .like(OperationLog::getDescription, trimToNull(query.getKeyword()))
                        .or().like(OperationLog::getTargetType, trimToNull(query.getKeyword())))
                .orderByDesc(OperationLog::getCreatedAt)
                .orderByDesc(OperationLog::getId);

        IPage<OperationLog> entityPage = operationLogMapper.selectPage(new Page<>(pageNo, pageSize), wrapper);
        List<Long> userIds = entityPage.getRecords().stream()
                .map(OperationLog::getOperatorUserId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, SysUser> users = userIds.isEmpty()
                ? Collections.emptyMap()
                : sysUserMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(SysUser::getId, Function.identity()));

        Page<OperationLogVO> result = new Page<>(entityPage.getCurrent(), entityPage.getSize(), entityPage.getTotal());
        result.setRecords(entityPage.getRecords().stream().map(log -> toVO(log, users)).toList());
        return result;
    }

    @Override
    public void record(String moduleName, String operationType, String targetType, Long targetId,
                       String description, Object beforeData, Object afterData) {
        OperationLog log = new OperationLog();
        log.setOperatorUserId(SecurityUtil.getCurrentUserId());
        log.setModuleName(moduleName);
        log.setOperationType(operationType);
        log.setTargetType(targetType);
        log.setTargetId(targetId);
        log.setDescription(description);
        log.setBeforeData(toJson(beforeData));
        log.setAfterData(toJson(afterData));
        log.setClientIp(resolveClientIp());
        operationLogMapper.insert(log);
    }

    private OperationLogVO toVO(OperationLog log, Map<Long, SysUser> users) {
        OperationLogVO vo = new OperationLogVO();
        BeanUtils.copyProperties(log, vo);
        SysUser user = users.get(log.getOperatorUserId());
        if (user != null) {
            vo.setOperatorUsername(user.getUsername());
            vo.setOperatorName(user.getRealName());
        }
        return vo;
    }

    private String toJson(Object data) {
        if (data == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            throw new BusinessException("操作日志数据序列化失败");
        }
    }

    private String resolveClientIp() {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes)) {
            return null;
        }
        HttpServletRequest request = attributes.getRequest();
        String forwarded = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwarded)) {
            return forwarded.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        return StringUtils.hasText(realIp) ? realIp.trim() : request.getRemoteAddr();
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
