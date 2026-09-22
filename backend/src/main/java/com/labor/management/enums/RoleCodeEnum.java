package com.labor.management.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 角色编码枚举
 */
@Getter
@AllArgsConstructor
public enum RoleCodeEnum {

    SUPER_ADMIN("SUPER_ADMIN", "超级管理员"),
    TEACHER("TEACHER", "教师"),
    ASSISTANT("ASSISTANT", "助教");

    private final String code;
    private final String label;
}
