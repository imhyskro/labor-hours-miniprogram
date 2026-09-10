package com.labor.management.exception;

import com.labor.management.common.ResultCode;
import lombok.Getter;

/**
 * 业务异常
 *
 * <p>所有业务校验失败、权限不足、数据封存等场景统一抛出此异常，
 * 由 GlobalExceptionHandler 捕获并转换为 CommonResult 返回前端。</p>
 */
@Getter
public class BusinessException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final Integer code;

    public BusinessException(String message) {
        super(message);
        this.code = ResultCode.BUSINESS_ERROR.getCode();
    }

    public BusinessException(ResultCode resultCode) {
        super(resultCode.getMessage());
        this.code = resultCode.getCode();
    }

    public BusinessException(ResultCode resultCode, String message) {
        super(message);
        this.code = resultCode.getCode();
    }

    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
    }
}
