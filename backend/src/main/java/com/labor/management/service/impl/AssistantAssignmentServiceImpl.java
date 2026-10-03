package com.labor.management.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.labor.management.exception.BusinessException;
import com.labor.management.service.AssistantAssignmentService;
import com.labor.management.util.SecurityUtil;
import com.labor.management.vo.AssistantVO;
import com.labor.management.vo.MasterListViewVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.sql.Statement;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AssistantAssignmentServiceImpl implements AssistantAssignmentService {
    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;
    @Value("${app.default-password:cdjcc123456}") private String defaultPassword;

    @Override
    public IPage<AssistantVO> pageAssistants(Integer page, Integer size, String keyword) {
        int current=page==null||page<1?1:page, pageSize=size==null||size<1?10:size;
        String filter=""; List<Object> args=new ArrayList<>();
        if(StringUtils.hasText(keyword)){filter=" AND (p.student_no_snapshot LIKE ? OR p.student_name_snapshot LIKE ?)";String k="%"+keyword.trim()+"%";args.add(k);args.add(k);}
        Long total=jdbcTemplate.queryForObject("SELECT COUNT(*) FROM assistant_profile p WHERE p.status='ACTIVE'"+filter,Long.class,args.toArray());
        List<Object> queryArgs=new ArrayList<>(args);queryArgs.add((current-1)*pageSize);queryArgs.add(pageSize);
        List<AssistantVO> rows=jdbcTemplate.query("""
                SELECT p.student_id id,p.student_no_snapshot,p.student_name_snapshot,p.gender_snapshot,p.admin_class_snapshot,
                       e.company_name_snapshot,e.class_code_snapshot,CONCAT(e.company_name_snapshot,'-',e.class_code_snapshot) class_name,
                       e.in_class_no,e.section_code,CASE WHEN p.user_id IS NULL THEN 0 ELSE 1 END has_account,
                       COUNT(DISTINCT a.teaching_group_id) assigned_count,
                       GROUP_CONCAT(DISTINCT CONCAT(a.company_name_snapshot,'-',a.class_code_snapshot) ORDER BY a.teaching_group_id SEPARATOR '、') assigned_names
                FROM assistant_profile p LEFT JOIN student_course_enrollment e ON e.student_id=p.student_id AND e.status='ACTIVE'
                LEFT JOIN assistant_group_assignment a ON a.assistant_profile_id=p.id AND a.unassigned_at IS NULL
                WHERE p.status='ACTIVE'
                """+filter+" GROUP BY p.id,e.id ORDER BY p.student_no_snapshot LIMIT ?,?",(rs,n)->{
            AssistantVO vo=new AssistantVO();vo.setId((Long)rs.getObject("id"));vo.setStudentId(rs.getString("student_no_snapshot"));
            vo.setName(rs.getString("student_name_snapshot"));vo.setGender(genderNumber(rs.getString("gender_snapshot")));
            vo.setOriginalMajor(rs.getString("admin_class_snapshot"));vo.setCompanyName(rs.getString("company_name_snapshot"));
            vo.setClassCode(rs.getString("class_code_snapshot"));vo.setClassName(rs.getString("class_name"));
            vo.setStudentNoInClass((Integer)rs.getObject("in_class_no"));vo.setFullNo(rs.getString("section_code"));vo.setHasAccount(rs.getInt("has_account"));
            vo.setAssignedClassCount(rs.getLong("assigned_count"));vo.setAssignedClassNames(rs.getString("assigned_names"));return vo;
        },queryArgs.toArray());
        Page<AssistantVO> result=new Page<>(current,pageSize,total==null?0:total);result.setRecords(rows);return result;
    }

    @Override
    public List<Long> getAssistantClassIds(Long studentId) {
        return jdbcTemplate.queryForList("""
                SELECT a.teaching_group_id FROM assistant_profile p JOIN assistant_group_assignment a ON a.assistant_profile_id=p.id
                WHERE p.student_id=? AND p.status='ACTIVE' AND a.unassigned_at IS NULL ORDER BY a.teaching_group_id
                """,Long.class,studentId);
    }

    @Override
    @Transactional
    public void assignClasses(Long studentId,List<Long> classIds){
        Long profileId=activeProfile(studentId);Set<Long> selected=new LinkedHashSet<>();
        if(classIds!=null)for(Long id:classIds){if(id==null)continue;Integer n=jdbcTemplate.queryForObject("SELECT COUNT(*) FROM teaching_group WHERE id=? AND status<>'ARCHIVED'",Integer.class,id);if(n==null||n==0)throw new BusinessException("班级不存在："+id);selected.add(id);}
        List<Long> current=getAssistantClassIds(studentId);
        for(Long id:current)if(!selected.contains(id))jdbcTemplate.update("UPDATE assistant_group_assignment SET unassigned_at=CURRENT_TIMESTAMP(3) WHERE assistant_profile_id=? AND teaching_group_id=? AND unassigned_at IS NULL",profileId,id);
        Long operator=SecurityUtil.getCurrentUserId();
        for(Long id:selected)if(!current.contains(id))jdbcTemplate.update("""
                INSERT INTO assistant_group_assignment(assistant_profile_id,teaching_group_id,assistant_no_snapshot,
                  assistant_name_snapshot,company_name_snapshot,class_code_snapshot,assigned_by_user_id)
                SELECT p.id,g.id,p.student_no_snapshot,p.student_name_snapshot,g.company_name_snapshot,g.class_code,?
                FROM assistant_profile p JOIN teaching_group g ON g.id=? WHERE p.id=?
                """,operator,id,profileId);
    }

    @Override
    @Transactional
    public void revokeAssistant(Long studentId){
        Long profileId=activeProfile(studentId);
        jdbcTemplate.update("UPDATE assistant_group_assignment SET unassigned_at=CURRENT_TIMESTAMP(3) WHERE assistant_profile_id=? AND unassigned_at IS NULL",profileId);
        Long userId=jdbcTemplate.query("SELECT user_id FROM assistant_profile WHERE id=?",rs->rs.next()?(Long)rs.getObject(1):null,profileId);
        jdbcTemplate.update("UPDATE assistant_profile SET status='INACTIVE' WHERE id=?",profileId);
        if(userId!=null){jdbcTemplate.update("DELETE ur FROM user_role ur JOIN sys_role r ON r.id=ur.role_id WHERE ur.user_id=? AND r.role_code='ASSISTANT'",userId);
            Integer roles=jdbcTemplate.queryForObject("SELECT COUNT(*) FROM user_role WHERE user_id=?",Integer.class,userId);if(roles==null||roles==0)jdbcTemplate.update("UPDATE sys_user SET status='DISABLED' WHERE id=?",userId);}
    }

    @Override
    @Transactional
    public void setIdentity(Long studentId,boolean isAssistant){if(!isAssistant){revokeAssistant(studentId);return;}promote(studentId);}

    private void promote(Long studentId){
        List<Map<String,Object>> students=jdbcTemplate.queryForList("""
                SELECT s.id,s.student_no,s.student_name,s.gender,COALESCE(ac.administrative_class_name,'未填写') admin_class
                FROM student s LEFT JOIN administrative_class ac ON ac.id=s.administrative_class_id WHERE s.id=?
                """,studentId);
        if(students.isEmpty())throw new BusinessException("学生不存在");Map<String,Object>s=students.get(0);String no=(String)s.get("student_no");
        Long roleId=jdbcTemplate.query("SELECT id FROM sys_role WHERE role_code='ASSISTANT'",rs->rs.next()?rs.getLong(1):null);if(roleId==null)throw new BusinessException("V2 数据库未初始化 ASSISTANT 角色");
        Long userId=jdbcTemplate.query("SELECT id FROM sys_user WHERE username=?",rs->rs.next()?rs.getLong(1):null,no);
        if(userId==null){var keys=new org.springframework.jdbc.support.GeneratedKeyHolder();jdbcTemplate.update(c->{var ps=c.prepareStatement("INSERT INTO sys_user(username,password_hash,real_name,status,first_login) VALUES (?,?,?,'ACTIVE',TRUE)",Statement.RETURN_GENERATED_KEYS);ps.setString(1,no);ps.setString(2,passwordEncoder.encode(defaultPassword));ps.setString(3,(String)s.get("student_name"));return ps;},keys);userId=Objects.requireNonNull(keys.getKey()).longValue();}
        else jdbcTemplate.update("UPDATE sys_user SET real_name=?,status='ACTIVE' WHERE id=?",s.get("student_name"),userId);
        jdbcTemplate.update("INSERT IGNORE INTO user_role(user_id,role_id) VALUES (?,?)",userId,roleId);
        Integer profileCount=jdbcTemplate.queryForObject("SELECT COUNT(*) FROM assistant_profile WHERE student_no_snapshot=?",Integer.class,no);
        if(profileCount!=null&&profileCount>0)jdbcTemplate.update("""
                UPDATE assistant_profile SET student_id=?,user_id=?,student_name_snapshot=?,gender_snapshot=?,admin_class_snapshot=?,status='ACTIVE'
                WHERE student_no_snapshot=?
                """,studentId,userId,s.get("student_name"),s.get("gender"),s.get("admin_class"),no);
        else jdbcTemplate.update("""
                INSERT INTO assistant_profile(student_id,user_id,student_no_snapshot,student_name_snapshot,gender_snapshot,admin_class_snapshot,status)
                VALUES (?,?,?,?,?,?,'ACTIVE')
                """,studentId,userId,no,s.get("student_name"),s.get("gender"),s.get("admin_class"));
    }

    @Override
    public List<MasterListViewVO> getUnassignedAssistants(){
        return jdbcTemplate.query("""
                SELECT p.student_id,p.student_no_snapshot,p.student_name_snapshot,p.gender_snapshot,p.admin_class_snapshot,
                       e.teaching_group_id,e.company_name_snapshot,e.class_code_snapshot,e.in_class_no,e.section_code,p.user_id
                FROM assistant_profile p LEFT JOIN student_course_enrollment e ON e.student_id=p.student_id AND e.status='ACTIVE'
                WHERE p.status='ACTIVE' AND NOT EXISTS(SELECT 1 FROM assistant_group_assignment a WHERE a.assistant_profile_id=p.id AND a.unassigned_at IS NULL)
                ORDER BY p.student_no_snapshot
                """,(rs,n)->{
            MasterListViewVO vo=new MasterListViewVO();vo.setId((Long)rs.getObject("student_id"));vo.setStudentId(rs.getString("student_no_snapshot"));vo.setName(rs.getString("student_name_snapshot"));
            vo.setGender(genderNumber(rs.getString("gender_snapshot")));vo.setClassId((Long)rs.getObject("teaching_group_id"));vo.setCompanyName(rs.getString("company_name_snapshot"));vo.setClassCode(rs.getString("class_code_snapshot"));
            vo.setClassName(rs.getString("company_name_snapshot")+"-"+rs.getString("class_code_snapshot"));vo.setStudentNoInClass((Integer)rs.getObject("in_class_no"));vo.setFullNo(rs.getString("section_code"));
            vo.setOriginalMajor(rs.getString("admin_class_snapshot"));vo.setIsAssistant(1);vo.setIdentity("助教");vo.setHasAccount(rs.getObject("user_id")==null?0:1);return vo;
        });
    }

    private Long activeProfile(Long studentId){Long id=jdbcTemplate.query("SELECT id FROM assistant_profile WHERE student_id=? AND status='ACTIVE'",rs->rs.next()?rs.getLong(1):null,studentId);if(id==null)throw new BusinessException("该学生不是有效助教");return id;}
    private int genderNumber(String v){return "男".equals(v)?1:"女".equals(v)?2:0;}
}
