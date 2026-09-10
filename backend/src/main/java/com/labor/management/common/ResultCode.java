package com.labor.management.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 统一返回结果状态码
 *
 * <p>阶段0仅定义基础状态码，业务状态码将在后续阶段按需补充。</p>
 */
@Getter
@AllArgsConstructor
public enum ResultCode {

    SUCCESS(200, "操作成功"),
    FAILED(500, "操作失败"),

    PARAM_ERROR(400, "参数校验失败"),
    UNAUTHORIZED(401, "未登录或登录已过期"),
    FORBIDDEN(403, "无操作权限"),
    NOT_FOUND(404, "资源不存在"),

    BUSINESS_ERROR(1000, "业务异常"),
    DATA_SEALED(1001, "数据已封存，不可直接修改"),
    PERMISSION_DENIED(1002, "无权操作该班级数据");

    private final Integer code;
    private final String message;
}
