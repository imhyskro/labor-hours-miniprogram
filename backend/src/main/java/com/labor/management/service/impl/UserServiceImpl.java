package com.labor.management.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.labor.management.dto.UserCreateDTO;
import com.labor.management.dto.UserQueryDTO;
import com.labor.management.exception.BusinessException;
import com.labor.management.service.ClassesService;
import com.labor.management.service.UserService;
import com.labor.management.util.SecurityUtil;
import com.labor.management.vo.ClassVO;
import com.labor.management.vo.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final JdbcTemplate jdbcTemplate;
    private final NamedParameterJdbcTemplate namedJdbcTemplate;
    private final PasswordEncoder passwordEncoder;
    private final ClassesService classesService;
    @Value("${app.default-password:cdjcc123456}") private String defaultPassword;

    @Override
    public IPage<UserVO> pageQuery(UserQueryDTO dto) {
        int current = dto.getPage() == null || dto.getPage() < 1 ? 1 : dto.getPage();
        int size = dto.getSize() == null || dto.getSize() < 1 ? 10 : dto.getSize();
        MapSqlParameterSource params = new MapSqlParameterSource().addValue("offset", (current-1)*size).addValue("size", size);
        String where = userWhere(dto, params);
        Long total = namedJdbcTemplate.queryForObject("SELECT COUNT(DISTINCT u.id) FROM sys_user u LEFT JOIN user_role ur ON ur.user_id=u.id LEFT JOIN sys_role r ON r.id=ur.role_id WHERE 1=1" + where, params, Long.class);
        List<UserVO> rows = namedJdbcTemplate.query("""
                SELECT u.id,u.username,u.real_name,u.status,u.first_login,u.last_password_change_at,u.created_at,
                       ap.student_id,GROUP_CONCAT(DISTINCT CASE WHEN r.role_code='ADMIN' THEN 'SUPER_ADMIN' ELSE r.role_code END ORDER BY r.id) roles
                FROM sys_user u LEFT JOIN user_role ur ON ur.user_id=u.id LEFT JOIN sys_role r ON r.id=ur.role_id
                LEFT JOIN assistant_profile ap ON ap.user_id=u.id
                WHERE 1=1
                """ + where + " GROUP BY u.id,ap.student_id ORDER BY u.id DESC LIMIT :offset,:size", params, this::mapUser);
        Page<UserVO> result = new Page<>(current,size,total == null ? 0 : total); result.setRecords(rows); return result;
    }

    @Override
    public UserVO getById(Long id) {
        List<UserVO> rows = jdbcTemplate.query("""
                SELECT u.id,u.username,u.real_name,u.status,u.first_login,u.last_password_change_at,u.created_at,
                       ap.student_id,GROUP_CONCAT(DISTINCT CASE WHEN r.role_code='ADMIN' THEN 'SUPER_ADMIN' ELSE r.role_code END ORDER BY r.id) roles
                FROM sys_user u LEFT JOIN user_role ur ON ur.user_id=u.id LEFT JOIN sys_role r ON r.id=ur.role_id
                LEFT JOIN assistant_profile ap ON ap.user_id=u.id WHERE u.id=? GROUP BY u.id,ap.student_id
                """, this::mapUser, id);
        if (rows.isEmpty()) throw new BusinessException("用户不存在"); return rows.get(0);
    }

    @Override
    public void updateStatus(Long userId, Integer status) {
        protectSelf(userId); requireUser(userId);
        jdbcTemplate.update("UPDATE sys_user SET status=? WHERE id=?", status != null && status == 1 ? "ACTIVE" : "DISABLED", userId);
    }

    @Override
    public void resetPassword(Long userId) {
        protectSelf(userId); requireUser(userId);
        jdbcTemplate.update("UPDATE sys_user SET password_hash=?,first_login=TRUE,last_password_change_at=NULL WHERE id=?",
                passwordEncoder.encode(defaultPassword), userId);
    }

    @Override
    @Transactional
    public Long createTeacher(UserCreateDTO dto) {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM sys_user WHERE username=?", Integer.class, dto.getUsername());
        if (count != null && count > 0) throw new BusinessException("用户名已存在");
        Long roleId = jdbcTemplate.query("SELECT id FROM sys_role WHERE role_code='TEACHER'", rs -> rs.next()?rs.getLong(1):null);
        if (roleId == null) throw new BusinessException("V2 数据库未初始化 TEACHER 角色");
        var keys = new org.springframework.jdbc.support.GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            var ps=connection.prepareStatement("INSERT INTO sys_user(username,password_hash,real_name,status,first_login) VALUES (?,?,?,'ACTIVE',TRUE)", Statement.RETURN_GENERATED_KEYS);
            ps.setString(1,dto.getUsername()); ps.setString(2,passwordEncoder.encode(defaultPassword)); ps.setString(3,dto.getRealName()); return ps;
        }, keys);
        if (keys.getKey()==null) throw new BusinessException("教师账号创建失败");
        Long id=keys.getKey().longValue(); jdbcTemplate.update("INSERT INTO user_role(user_id,role_id) VALUES (?,?)",id,roleId); return id;
    }

    @Override
    public List<ClassVO> getTeacherClasses(Long userId) {
        requireTeacher(userId);
        List<Long> ids=jdbcTemplate.queryForList("SELECT id FROM teaching_group WHERE current_teacher_user_id=? AND status<>'ARCHIVED' ORDER BY id",Long.class,userId);
        return ids.stream().map(id->classesService.getById(id,null)).toList();
    }

    @Override
    @Transactional
    public void assignClassesToTeacher(Long userId, List<Long> classIds) {
        requireTeacher(userId);
        Set<Long> selected=new LinkedHashSet<>();
        if(classIds!=null) for(Long id:classIds){ if(id==null) continue; classesService.getById(id,null); selected.add(id); }
        Long operator=SecurityUtil.getCurrentUserId();
        List<Long> current=jdbcTemplate.queryForList("SELECT id FROM teaching_group WHERE current_teacher_user_id=?",Long.class,userId);
        for(Long id:current) if(!selected.contains(id)){
            jdbcTemplate.update("UPDATE teacher_assignment_history SET unassigned_at=CURRENT_TIMESTAMP(3) WHERE teaching_group_id=? AND unassigned_at IS NULL",id);
            jdbcTemplate.update("UPDATE teaching_group SET current_teacher_user_id=NULL WHERE id=?",id);
        }
        for(Long id:selected){
            Long old=jdbcTemplate.query("SELECT current_teacher_user_id FROM teaching_group WHERE id=?",rs->rs.next()?(Long)rs.getObject(1):null,id);
            if(Objects.equals(old,userId)) continue;
            jdbcTemplate.update("UPDATE teacher_assignment_history SET unassigned_at=CURRENT_TIMESTAMP(3) WHERE teaching_group_id=? AND unassigned_at IS NULL",id);
            jdbcTemplate.update("UPDATE teaching_group SET current_teacher_user_id=? WHERE id=?",userId,id);
            jdbcTemplate.update("""
                    INSERT INTO teacher_assignment_history(teaching_group_id,teacher_user_id,teacher_username_snapshot,
                      teacher_name_snapshot,assigned_at,assigned_by_user_id)
                    SELECT ?,id,username,real_name,CURRENT_TIMESTAMP(3),? FROM sys_user WHERE id=?
                    """,id,operator,userId);
        }
    }

    private UserVO mapUser(ResultSet rs,int n)throws SQLException{
        UserVO vo=new UserVO(); vo.setId(rs.getLong("id"));vo.setUsername(rs.getString("username"));vo.setRealName(rs.getString("real_name"));
        vo.setStudentId((Long)rs.getObject("student_id")); String roles=rs.getString("roles");vo.setRoles(roles==null?List.of():Arrays.asList(roles.split(",")));
        vo.setStatus("ACTIVE".equals(rs.getString("status"))?1:0);vo.setFirstLogin(rs.getBoolean("first_login"));
        if(rs.getTimestamp("last_password_change_at")!=null)vo.setLastPasswordChangeTime(rs.getTimestamp("last_password_change_at").toLocalDateTime());
        vo.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());return vo;
    }
    private String userWhere(UserQueryDTO dto,MapSqlParameterSource p){
        StringBuilder w=new StringBuilder();
        if(StringUtils.hasText(dto.getKeyword())){p.addValue("kw","%"+dto.getKeyword().trim()+"%");w.append(" AND (u.username LIKE :kw OR u.real_name LIKE :kw)");}
        if(dto.getStatus()!=null){p.addValue("status",dto.getStatus()==1?"ACTIVE":"DISABLED");w.append(" AND u.status=:status");}
        if(StringUtils.hasText(dto.getRole())){p.addValue("role","SUPER_ADMIN".equals(dto.getRole())?"ADMIN":dto.getRole());w.append(" AND r.role_code=:role");}
        return w.toString();
    }
    private void protectSelf(Long id){if(Objects.equals(SecurityUtil.getCurrentUserId(),id))throw new BusinessException("不能停用或重置自己的账号");}
    private void requireUser(Long id){Integer n=jdbcTemplate.queryForObject("SELECT COUNT(*) FROM sys_user WHERE id=?",Integer.class,id);if(n==null||n==0)throw new BusinessException("用户不存在");}
    private void requireTeacher(Long id){Integer n=jdbcTemplate.queryForObject("""
            SELECT COUNT(*) FROM sys_user u JOIN user_role ur ON ur.user_id=u.id JOIN sys_role r ON r.id=ur.role_id
            WHERE u.id=? AND r.role_code='TEACHER'
            """,Integer.class,id);if(n==null||n==0)throw new BusinessException("用户不存在或不是教师");}
}
