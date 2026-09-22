package com.labor.management.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.labor.management.entity.UserRole;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 用户角色关联 Mapper
 */
@Mapper
public interface UserRoleMapper extends BaseMapper<UserRole> {

    /**
     * 按 (userId, roleId) 查询，包含已逻辑删除的记录（绕过 @TableLogic）
     *
     * <p>用于复活助教身份时判断 user_role 关联是否已存在历史记录（含 deleted=1）。</p>
     */
    @Select("SELECT * FROM user_role WHERE user_id=#{userId} AND role_id=#{roleId} LIMIT 1")
    UserRole selectByUserAndRoleIncludeDeleted(@Param("userId") Long userId,
                                               @Param("roleId") Long roleId);

    /**
     * 复活 user_role 关联：把 deleted=1 的记录 UPDATE 为 deleted=0
     */
    @Update("UPDATE user_role SET deleted=0, updated_at=NOW() WHERE user_id=#{userId} AND role_id=#{roleId}")
    int reviveByUserAndRole(@Param("userId") Long userId, @Param("roleId") Long roleId);
}
