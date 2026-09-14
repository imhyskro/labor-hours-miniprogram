package com.labor.management.service;

import java.util.List;

/**
 * 数据范围服务
 *
 * <p>用于实现按角色的数据隔离：</p>
 * <ul>
 *   <li>超级管理员：不限制（{@link #getCurrentUserScopeClassIds()} 返回 null）</li>
 *   <li>教师：只能查看/操作其负责班级范围内的数据（返回 teacher_class 关联的 class_id 列表）</li>
 *   <li>助教：本服务不处理（已被 @PreAuthorize 在 Controller 层拦截）</li>
 * </ul>
 */
public interface DataScopeService {

    /**
     * 当前登录用户是否超级管理员
     */
    boolean isCurrentUserSuperAdmin();

    /**
     * 获取当前用户的数据范围（班级ID列表）
     *
     * @return null 表示不限制（超级管理员）；非空 List 表示限制为这些班级（教师）；
     *         空 List 表示用户无任何负责班级（理论不应发生，老师无负责班级将查不到任何数据）
     */
    List<Long> getCurrentUserScopeClassIds();
}
