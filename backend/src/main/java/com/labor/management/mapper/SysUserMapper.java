package com.labor.management.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.labor.management.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

/**
 * 系统用户 Mapper
 */
@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {

    /**
     * 按 username 查询 sys_user，包含已逻辑删除的记录（绕过 @TableLogic 自动过滤）
     *
     * <p>用于"复活"助教账号：助教取消身份时账号被逻辑删除（deleted=1），
     * 但 username 仍占 UNIQUE 约束；重新设置助教身份时需先查询到旧账号并复活，
     * 不能再 INSERT 否则会触发 uk_sys_user_username 唯一键冲突。</p>
     */
    @Select("SELECT * FROM sys_user WHERE username = #{username} LIMIT 1")
    SysUser selectByUsernameIncludeDeleted(@Param("username") String username);

    /**
     * 复活并重置账号：直接用原生 SQL UPDATE 绕过 @TableLogic 自动加 deleted=0 过滤
     *
     * <p>把已逻辑删除的账号（deleted=1）复活为 deleted=0，并重置密码、姓名、状态、
     * 首登标志、student_id、最后密码修改时间。</p>
     */
    @Update("UPDATE sys_user SET password_hash=#{passwordHash}, real_name=#{realName}, " +
            "status=1, first_login=1, student_id=#{studentId}, deleted=0, " +
            "last_password_change_time=#{lastPasswordChangeTime}, updated_at=NOW() " +
            "WHERE id=#{id}")
    int reviveAndReset(@Param("id") Long id,
                       @Param("passwordHash") String passwordHash,
                       @Param("realName") String realName,
                       @Param("studentId") Long studentId,
                       @Param("lastPasswordChangeTime") LocalDateTime lastPasswordChangeTime);
}
