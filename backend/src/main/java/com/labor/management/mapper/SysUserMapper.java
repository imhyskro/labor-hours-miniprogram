package com.labor.management.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
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
     * 按角色 + 关键词 + 状态分页查询（用于权限分配页按 TEACHER 角色过滤老师列表）
     *
     * <p>参数化绑定 roleCode，不拼字符串；roleCode 为空则不按角色过滤。
     * roleCode 不存在时子查询返回空集，自然得到空页。</p>
     * <p>注意：原生 SQL 不走 @TableLogic，必须显式加 s.deleted = 0。</p>
     */
    @Select("<script>" +
            "SELECT s.* FROM sys_user s WHERE s.deleted = 0 " +
            "<if test='keyword != null and keyword != \"\"'>" +
            "  AND (s.username LIKE CONCAT('%', #{keyword}, '%') " +
            "       OR s.real_name LIKE CONCAT('%', #{keyword}, '%')) " +
            "</if>" +
            "<if test='status != null'> AND s.status = #{status} </if>" +
            "<if test='roleCode != null and roleCode != \"\"'>" +
            "  AND s.id IN (SELECT ur.user_id FROM user_role ur " +
            "              JOIN sys_role sr ON ur.role_id = sr.id " +
            "              WHERE sr.deleted = 0 AND sr.role_code = #{roleCode}) " +
            "</if>" +
            "ORDER BY s.created_at DESC" +
            "</script>")
    IPage<SysUser> selectPageByRole(IPage<SysUser> page,
                                    @Param("keyword") String keyword,
                                    @Param("status") Integer status,
                                    @Param("roleCode") String roleCode);

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
