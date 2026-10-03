package com.labor.management.exception;

import com.labor.management.common.CommonResult;
import com.labor.management.common.ResultCode;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * 全局异常处理器
 *
 * <p>统一捕获并处理各类异常，转换为 CommonResult 结构返回前端，
 * 遵循编码规范中的"统一异常处理"要求。</p>
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 业务异常 */
    @ExceptionHandler(BusinessException.class)
    public CommonResult<Void> handleBusinessException(BusinessException e) {
        log.warn("业务异常: code={}, message={}", e.getCode(), e.getMessage());
        return CommonResult.failed(e.getCode(), e.getMessage());
    }

    /** 参数校验异常 - @RequestBody @Valid */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public CommonResult<Void> handleMethodArgumentNotValid(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        log.warn("参数校验失败: {}", message);
        return CommonResult.failed(ResultCode.PARAM_ERROR, message);
    }

    /** 参数校验异常 - @ModelAttribute */
    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public CommonResult<Void> handleBindException(BindException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        log.warn("参数绑定失败: {}", message);
        return CommonResult.failed(ResultCode.PARAM_ERROR, message);
    }

    /** 参数校验异常 - @RequestParam/@PathVariable 校验 */
    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public CommonResult<Void> handleConstraintViolation(ConstraintViolationException e) {
        String message = e.getConstraintViolations().stream()
                .map(v -> v.getMessage())
                .collect(Collectors.joining("; "));
        log.warn("参数约束校验失败: {}", message);
        return CommonResult.failed(ResultCode.PARAM_ERROR, message);
    }

    /** 权限不足 */
    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public CommonResult<Void> handleAccessDenied(AccessDeniedException e) {
        log.warn("权限不足: {}", e.getMessage());
        return CommonResult.failed(ResultCode.FORBIDDEN);
    }

    /** 未认证 */
    @ExceptionHandler(AuthenticationException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public CommonResult<Void> handleAuthentication(AuthenticationException e) {
        log.warn("未认证: {}", e.getMessage());
        return CommonResult.failed(ResultCode.UNAUTHORIZED);
    }

    /** 登录失败 - 用户名或密码错误 */
    @ExceptionHandler(org.springframework.security.authentication.BadCredentialsException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public CommonResult<Void> handleBadCredentials(
            org.springframework.security.authentication.BadCredentialsException e) {
        log.warn("登录失败，用户名或密码错误: {}", e.getMessage());
        return CommonResult.failed(ResultCode.UNAUTHORIZED.getCode(), "用户名或密码错误");
    }

    /** 兜底异常 */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public CommonResult<Void> handleException(Exception e) {
        log.error("系统异常", e);
        return CommonResult.failed(ResultCode.FAILED.getCode(), "系统异常，请联系管理员");
    }
}
