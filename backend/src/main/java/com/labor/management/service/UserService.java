package com.labor.management.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.labor.management.dto.UserCreateDTO;
import com.labor.management.dto.UserQueryDTO;
import com.labor.management.vo.ClassVO;
import com.labor.management.vo.UserVO;

import java.util.List;

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

    /**
     * 创建教师账号
     * <p>校验用户名不重复，密码用默认值 cdjcc123456（BCrypt），
     * first_login=1 强制首次登录改密，自动绑定 TEACHER 角色。</p>
     *
     * @return 新创建的 userId
     */
    Long createTeacher(UserCreateDTO dto);

    /**
     * 查询教师负责的班级列表
     *
     * @param userId 教师 sys_user 主键（必须是 TEACHER 角色）
     */
    List<ClassVO> getTeacherClasses(Long userId);

    /**
     * 设置教师负责的班级（全量覆盖：物理清旧 + 重新插入）
     *
     * @param userId    教师 sys_user 主键
     * @param classIds  负责班级ID列表（空列表表示清空）
     */
    void assignClassesToTeacher(Long userId, List<Long> classIds);
}
