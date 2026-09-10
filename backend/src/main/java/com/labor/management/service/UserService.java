package com.labor.management.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.labor.management.dto.UserQueryDTO;
import com.labor.management.vo.UserVO;

/**
 * 用户 Service 接口（仅超级管理员可操作）
 */
public interface UserService {

    /**
     * 分页查询用户列表，关联 user_role 和 sys_role 获取角色列表
     */
    IPage<UserVO> pageQuery(UserQueryDTO queryDTO);

    /**
     * 根据 ID 查询用户（含角色）
     */
    UserVO getById(Long id);

    /**
     * 启用/停用用户
     *
     * @param userId 目标用户 ID
     * @param status 1=启用, 0=停用
     */
    void updateStatus(Long userId, Integer status);

    /**
     * 重置密码为初始默认值 cdjcc123456，同时将 first_login 置为 1
     */
    void resetPassword(Long userId);
}
