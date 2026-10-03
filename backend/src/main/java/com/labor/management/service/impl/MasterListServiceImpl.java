package com.labor.management.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.labor.management.service.MasterListService;
import com.labor.management.vo.MasterListViewVO;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MasterListServiceImpl implements MasterListService {
    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Override
    public IPage<MasterListViewVO> getMasterList(Integer page,Integer size,String keyword,Long companyId,Long classId,String identity,List<Long> scope){
        int current=page==null||page<1?1:page,pageSize=size==null||size<1?10:size;
        MapSqlParameterSource p=new MapSqlParameterSource().addValue("offset",(current-1)*pageSize).addValue("size",pageSize);
        String where=where(keyword,companyId,classId,identity,scope,p);
        Long total=jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM student_course_enrollment e JOIN student s ON s.id=e.student_id
                LEFT JOIN teaching_group g ON g.id=e.teaching_group_id
                WHERE e.status<>'WITHDRAWN'
                """+where,p,Long.class);
        List<MasterListViewVO> rows=jdbcTemplate.query("""
                SELECT s.id,e.student_no_snapshot,e.student_name_snapshot,e.gender_snapshot,e.teaching_group_id,
                       g.company_id,e.company_name_snapshot,g.week,e.class_code_snapshot,e.in_class_no,e.section_code,e.admin_class_snapshot,
                       CASE WHEN ap.id IS NULL THEN 0 ELSE 1 END is_assistant,ap.user_id,
                       GROUP_CONCAT(DISTINCT CONCAT(aga.company_name_snapshot,'-',aga.class_code_snapshot) ORDER BY aga.teaching_group_id SEPARATOR '、') assigned_names
                FROM student_course_enrollment e JOIN student s ON s.id=e.student_id LEFT JOIN teaching_group g ON g.id=e.teaching_group_id
                LEFT JOIN assistant_profile ap ON ap.student_id=s.id AND ap.status='ACTIVE'
                LEFT JOIN assistant_group_assignment aga ON aga.assistant_profile_id=ap.id AND aga.unassigned_at IS NULL
                WHERE e.status<>'WITHDRAWN'
                """+where+" GROUP BY e.id,ap.id ORDER BY e.company_name_snapshot,e.class_code_snapshot,e.in_class_no LIMIT :offset,:size",p,this::map);
        Page<MasterListViewVO> result=new Page<>(current,pageSize,total==null?0:total);result.setRecords(rows);return result;
    }

    private String where(String keyword,Long companyId,Long classId,String identity,List<Long> scope,MapSqlParameterSource p){
        StringBuilder w=new StringBuilder();
        if(StringUtils.hasText(keyword)){p.addValue("kw","%"+keyword.trim()+"%");w.append(" AND (e.student_no_snapshot LIKE :kw OR e.student_name_snapshot LIKE :kw)");}
        if(companyId!=null){p.addValue("companyId",companyId);w.append(" AND g.company_id=:companyId");}
        if(classId!=null){p.addValue("classId",classId);w.append(" AND e.teaching_group_id=:classId");}
        if("ASSISTANT".equalsIgnoreCase(identity))w.append(" AND EXISTS(SELECT 1 FROM assistant_profile x WHERE x.student_id=s.id AND x.status='ACTIVE')");
        else if("STUDENT".equalsIgnoreCase(identity))w.append(" AND NOT EXISTS(SELECT 1 FROM assistant_profile x WHERE x.student_id=s.id AND x.status='ACTIVE')");
        if(scope!=null){if(scope.isEmpty())w.append(" AND 1=0");else{p.addValue("scope",scope);w.append(" AND e.teaching_group_id IN (:scope)");}}
        return w.toString();
    }
    private MasterListViewVO map(ResultSet rs,int n)throws SQLException{
        MasterListViewVO vo=new MasterListViewVO();vo.setId(rs.getLong("id"));vo.setStudentId(rs.getString("student_no_snapshot"));vo.setName(rs.getString("student_name_snapshot"));
        vo.setGender(gender(rs.getString("gender_snapshot")));vo.setClassId((Long)rs.getObject("teaching_group_id"));vo.setCompanyId((Long)rs.getObject("company_id"));vo.setCompanyName(rs.getString("company_name_snapshot"));
        vo.setWeek((Integer)rs.getObject("week"));String code=rs.getString("class_code_snapshot");vo.setClassCode(code);int[] periods=parse(code);vo.setStartSession(periods[0]);vo.setEndSession(periods[1]);
        vo.setClassName(rs.getString("company_name_snapshot")+"-"+code);vo.setStudentNoInClass(rs.getInt("in_class_no"));vo.setFullNo(rs.getString("section_code"));vo.setOriginalMajor(rs.getString("admin_class_snapshot"));
        int assistant=rs.getInt("is_assistant");vo.setIsAssistant(assistant);vo.setIdentity(assistant==1?"助教":"学生");vo.setAssignedClassNames(rs.getString("assigned_names"));vo.setHasAccount(rs.getObject("user_id")==null?0:1);return vo;
    }
    private int[] parse(String code){try{String[] p=code.split("-");return new int[]{Integer.parseInt(p[p.length-2]),Integer.parseInt(p[p.length-1])};}catch(Exception e){return new int[]{0,0};}}
    private int gender(String v){return "男".equals(v)?1:"女".equals(v)?2:0;}
}
