package com.labor.management.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 性别枚举
 */
@Getter
@AllArgsConstructor
public enum GenderEnum {

    MALE(1, "男"),
    FEMALE(2, "女");

    private final Integer code;
    private final String label;

    public static String getLabelByCode(Integer code) {
        if (code == null) return "未知";
        for (GenderEnum e : values()) {
            if (e.code.equals(code)) {
                return e.label;
            }
        }
        return "未知";
    }
}
