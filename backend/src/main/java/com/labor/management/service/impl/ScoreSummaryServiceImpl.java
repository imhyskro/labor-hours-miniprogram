package com.labor.management.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.labor.management.dto.ScoreSingleSaveDTO;
import com.labor.management.enums.ScoreType;
import com.labor.management.exception.BusinessException;
import com.labor.management.service.ScoreSummaryService;
import com.labor.management.vo.ScoreSummaryVO;
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
public class ScoreSummaryServiceImpl implements ScoreSummaryService {
    private final NamedParameterJdbcTemplate jdbcTemplate;
    @Override
    public IPage<ScoreSummaryVO> getSummaryPage(Integer page,Integer size,String keyword,Long companyId,Long classId,String identity,List<Long> scope){
        int current=page==null||page<1?1:page,pageSize=size==null||size<1?10:size;
        MapSqlParameterSource p=new MapSqlParameterSource().addValue("offset",(current-1)*pageSize).addValue("size",pageSize);
        String where=where(keyword,companyId,classId,identity,scope,p);
        Long total=jdbcTemplate.queryForObject("SELECT COUNT(*) FROM student_course_enrollment e JOIN student s ON s.id=e.student_id LEFT JOIN teaching_group g ON g.id=e.teaching_group_id WHERE e.status<>'WITHDRAWN'"+where,p,Long.class);
        List<ScoreSummaryVO> rows=jdbcTemplate.query("""
                SELECT s.id,e.student_no_snapshot,e.student_name_snapshot,e.gender_snapshot,e.teaching_group_id,g.company_id,
                       e.company_name_snapshot,g.week,e.class_code_snapshot,e.in_class_no,e.section_code,e.admin_class_snapshot,
                       CASE WHEN ap.id IS NULL THEN 0 ELSE 1 END is_assistant,ap.user_id,
                       ROUND(100*SUM(CASE WHEN sr.score_value IS NOT NULL THEN sr.score_value ELSE 0 END)
                         / NULLIF(SUM(CASE WHEN sr.score_value IS NOT NULL THEN cs.max_score ELSE 0 END),0),2) labor_score
                FROM student_course_enrollment e JOIN student s ON s.id=e.student_id LEFT JOIN teaching_group g ON g.id=e.teaching_group_id
                LEFT JOIN assistant_profile ap ON ap.student_id=s.id AND ap.status='ACTIVE'
                LEFT JOIN score_record sr ON sr.enrollment_id=e.id LEFT JOIN course_session cs ON cs.id=sr.course_session_id
                WHERE e.status<>'WITHDRAWN'
                """+where+" GROUP BY e.id,ap.id ORDER BY e.company_name_snapshot,e.class_code_snapshot,e.in_class_no LIMIT :offset,:size",p,this::map);
        Page<ScoreSummaryVO> result=new Page<>(current,pageSize,total==null?0:total);result.setRecords(rows);return result;
    }
    @Override public void manualFill(ScoreType type,ScoreSingleSaveDTO dto){throw new BusinessException("V2 仅允许按 course_session 写入 score_record，不能按旧版成绩类别手工补分");}
    private String where(String keyword,Long companyId,Long classId,String identity,List<Long> scope,MapSqlParameterSource p){
        StringBuilder w=new StringBuilder();if(StringUtils.hasText(keyword)){p.addValue("kw","%"+keyword.trim()+"%");w.append(" AND (e.student_no_snapshot LIKE :kw OR e.student_name_snapshot LIKE :kw)");}
        if(companyId!=null){p.addValue("companyId",companyId);w.append(" AND g.company_id=:companyId");}if(classId!=null){p.addValue("classId",classId);w.append(" AND e.teaching_group_id=:classId");}
        if("ASSISTANT".equalsIgnoreCase(identity))w.append(" AND EXISTS(SELECT 1 FROM assistant_profile x WHERE x.student_id=s.id AND x.status='ACTIVE')");else if("STUDENT".equalsIgnoreCase(identity))w.append(" AND NOT EXISTS(SELECT 1 FROM assistant_profile x WHERE x.student_id=s.id AND x.status='ACTIVE')");
        if(scope!=null){if(scope.isEmpty())w.append(" AND 1=0");else{p.addValue("scope",scope);w.append(" AND e.teaching_group_id IN (:scope)");}}return w.toString();
    }
    private ScoreSummaryVO map(ResultSet rs,int n)throws SQLException{
        ScoreSummaryVO vo=new ScoreSummaryVO();vo.setId(rs.getLong("id"));vo.setStudentId(rs.getString("student_no_snapshot"));vo.setName(rs.getString("student_name_snapshot"));vo.setGender(gender(rs.getString("gender_snapshot")));
        vo.setClassId((Long)rs.getObject("teaching_group_id"));vo.setCompanyId((Long)rs.getObject("company_id"));vo.setCompanyName(rs.getString("company_name_snapshot"));vo.setWeek((Integer)rs.getObject("week"));String code=rs.getString("class_code_snapshot");vo.setClassCode(code);
        int[] periods=parse(code);vo.setStartSession(periods[0]);vo.setEndSession(periods[1]);vo.setClassName(rs.getString("company_name_snapshot")+"-"+code);vo.setStudentNoInClass(rs.getInt("in_class_no"));vo.setFullNo(rs.getString("section_code"));vo.setOriginalMajor(rs.getString("admin_class_snapshot"));
        int assistant=rs.getInt("is_assistant");vo.setIsAssistant(assistant);vo.setIdentity(assistant==1?"助教":"学生");vo.setHasAccount(rs.getObject("user_id")==null?0:1);vo.setAttendanceScore(rs.getBigDecimal("labor_score"));
        vo.setMissingItems(vo.getAttendanceScore()==null?List.of("ATTENDANCE","COURSE_REPORT","THEORY","PRACTICE"):List.of("COURSE_REPORT","THEORY","PRACTICE"));vo.setFinalScore(null);return vo;
    }
    private int[] parse(String code){try{String[] p=code.split("-");return new int[]{Integer.parseInt(p[p.length-2]),Integer.parseInt(p[p.length-1])};}catch(Exception e){return new int[]{0,0};}}
    private int gender(String v){return "男".equals(v)?1:"女".equals(v)?2:0;}
}
