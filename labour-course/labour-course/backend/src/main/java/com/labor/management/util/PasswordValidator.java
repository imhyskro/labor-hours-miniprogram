package com.labor.management.util;

import com.labor.management.exception.BusinessException;

import java.util.regex.Pattern;

/**
 * 密码复杂度校验工具
 *
 * <p>规则：长度 >= 8 位，必须包含大写字母、小写字母、数字、特殊字符中的至少 3 种。</p>
 */
public final class PasswordValidator {

    private PasswordValidator() {
    }

    private static final int MIN_LENGTH = 8;
    private static final int MIN_CATEGORY_COUNT = 3;

    private static final Pattern UPPER = Pattern.compile(".*[A-Z].*");
    private static final Pattern LOWER = Pattern.compile(".*[a-z].*");
    private static final Pattern DIGIT = Pattern.compile(".*\\d.*");
    private static final Pattern SPECIAL = Pattern.compile(".*[@$!%*?&].*");

    /**
     * 校验密码复杂度，不通过则抛出 BusinessException
     */
    public static void validate(String password) {
        if (password == null || password.isEmpty()) {
            throw new BusinessException("密码不能为空");
        }
        if (password.length() < MIN_LENGTH) {
            throw new BusinessException("密码长度不能少于 " + MIN_LENGTH + " 位");
        }

        int categoryCount = 0;
        if (UPPER.matcher(password).matches()) {
            categoryCount++;
        }
        if (LOWER.matcher(password).matches()) {
            categoryCount++;
        }
        if (DIGIT.matcher(password).matches()) {
            categoryCount++;
        }
        if (SPECIAL.matcher(password).matches()) {
            categoryCount++;
        }

        if (categoryCount < MIN_CATEGORY_COUNT) {
            throw new BusinessException(
                    "密码必须包含大写字母、小写字母、数字、特殊字符(@$!%*?&)中的至少 3 种");
        }
    }
}
