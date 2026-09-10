package com.labor.management.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 通用状态枚举（启用/停用）
 */
@Getter
@AllArgsConstructor
public enum StatusEnum {

    ENABLED(1, "启用"),
    DISABLED(0, "停用");

    private final Integer code;
    private final String label;

    public static String getLabelByCode(Integer code) {
        if (code == null) return "未知";
        for (StatusEnum e : values()) {
            if (e.code.equals(code)) {
                return e.label;
            }
        }
        return "未知";
    }
}
