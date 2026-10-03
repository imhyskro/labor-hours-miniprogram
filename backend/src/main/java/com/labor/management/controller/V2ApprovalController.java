package com.labor.management.controller;

import com.labor.management.common.CommonResult;
import com.labor.management.exception.BusinessException;
import com.labor.management.service.DataScopeService;
import com.labor.management.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/** Web-side approval, temporary unlock, action trail and score revision queries for V2. */
@RestController
@RequestMapping("/api/approvals")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('SUPER_ADMIN','TEACHER')")
public class V2ApprovalController {
    private final JdbcTemplate jdbcTemplate;
    private final DataScopeService dataScopeService;

    @GetMapping
    public CommonResult<List<Map<String,Object>>> list(@RequestParam(required=false)String status){
        expireWindows();
        String statusSql=status==null||status.isBlank()?"":" AND cr.status=?";
        List<Object> args=new java.util.ArrayList<>();if(!statusSql.isEmpty())args.add(status.trim().toUpperCase());
        String scopeSql="";List<Long> scope=dataScopeService.getCurrentUserScopeClassIds();
        if(scope!=null){if(scope.isEmpty())scopeSql=" AND 1=0";else{scopeSql=" AND ((cr.target_table='score_record' AND cs.teaching_group_id IN ("+placeholders(scope.size())+")) OR (cr.target_table='attendance_record' AND acs.teaching_group_id IN ("+placeholders(scope.size())+")))";args.addAll(scope);args.addAll(scope);}}
        return CommonResult.success(jdbcTemplate.queryForList("""
                SELECT cr.id,cr.request_no requestNo,cr.request_type requestType,cr.target_table targetTable,
                       cr.target_id targetId,cr.applicant_user_id applicantUserId,cr.applicant_name_snapshot applicantName,
                       cr.reason,cr.status,cr.approved_at approvedAt,cr.edit_window_expires_at editWindowExpiresAt,
                       cr.used_at usedAt,cr.created_at createdAt,
                       COALESCE(cs.teaching_group_id,acs.teaching_group_id) teachingGroupId,
                       COALESCE(cs.company_name_snapshot,acs.company_name_snapshot) companyName,
                       COALESCE(cs.class_code_snapshot,acs.class_code_snapshot) classCode
                FROM change_request cr
                LEFT JOIN score_record sr ON cr.target_table='score_record' AND sr.id=cr.target_id
                LEFT JOIN course_session cs ON cs.id=sr.course_session_id
                LEFT JOIN attendance_record ar ON cr.target_table='attendance_record' AND ar.id=cr.target_id
                LEFT JOIN course_session acs ON acs.id=ar.course_session_id
                WHERE 1=1
                """+statusSql+scopeSql+" ORDER BY cr.created_at DESC",args.toArray()));
    }

    @PostMapping("/{requestId}/decision")
    @Transactional
    public CommonResult<Void> decide(@PathVariable Long requestId,@RequestBody Map<String,Object> body){
        String decision=String.valueOf(body.get("decision")).trim().toUpperCase();
        if(!"APPROVE".equals(decision)&&!"REJECT".equals(decision))throw new BusinessException("decision 只能是 APPROVE 或 REJECT");
        List<Map<String,Object>> rows=jdbcTemplate.queryForList("SELECT * FROM change_request WHERE id=? FOR UPDATE",requestId);
        if(rows.isEmpty())throw new BusinessException("修改申请不存在");Map<String,Object> request=rows.get(0);
        if(!"PENDING".equals(request.get("status")))throw new BusinessException("只有待审批申请可以处理");
        Long current=SecurityUtil.getCurrentUserId();Object applicant=request.get("applicant_user_id");
        if(applicant instanceof Number&&current!=null&&current.equals(((Number)applicant).longValue()))throw new BusinessException("申请人不能审批自己的申请");
        assertScope((String)request.get("target_table"),((Number)request.get("target_id")).longValue());
        String comment=body.get("comment")==null?null:String.valueOf(body.get("comment"));
        if("APPROVE".equals(decision)){int hours=windowHours();LocalDateTime now=LocalDateTime.now();jdbcTemplate.update("UPDATE change_request SET status='APPROVED',approved_at=?,edit_window_expires_at=? WHERE id=?",now,now.plusHours(hours),requestId);insertAction(requestId,"APPROVE",comment);}
        else{jdbcTemplate.update("UPDATE change_request SET status='REJECTED' WHERE id=?",requestId);insertAction(requestId,"REJECT",comment);}
        return CommonResult.success();
    }

    @GetMapping("/{requestId}/actions")
    public CommonResult<List<Map<String,Object>>> actions(@PathVariable Long requestId){
        return CommonResult.success(jdbcTemplate.queryForList("""
                SELECT id,action,operator_user_id operatorUserId,operator_name_snapshot operatorName,
                       comment_text comment,action_at actionAt FROM approval_action
                WHERE change_request_id=? ORDER BY action_at,id
                """,requestId));
    }

    @GetMapping("/score-records/{scoreRecordId}/revisions")
    public CommonResult<List<Map<String,Object>>> revisions(@PathVariable Long scoreRecordId){
        assertScope("score_record",scoreRecordId);
        return CommonResult.success(jdbcTemplate.queryForList("""
                SELECT id,revision_no revisionNo,old_score_value oldScoreValue,old_score_mark oldScoreMark,
                       new_score_value newScoreValue,new_score_mark newScoreMark,change_reason changeReason,
                       change_request_id changeRequestId,changed_by_user_id changedByUserId,
                       changed_by_name_snapshot changedByName,changed_at changedAt
                FROM score_revision WHERE score_record_id=? ORDER BY revision_no
                """,scoreRecordId));
    }

    private void assertScope(String table,Long targetId){
        List<Long> scope=dataScopeService.getCurrentUserScopeClassIds();if(scope==null)return;
        Long groupId;if("score_record".equals(table))groupId=jdbcTemplate.query("SELECT cs.teaching_group_id FROM score_record r JOIN course_session cs ON cs.id=r.course_session_id WHERE r.id=?",rs->rs.next()?(Long)rs.getObject(1):null,targetId);
        else if("attendance_record".equals(table))groupId=jdbcTemplate.query("SELECT cs.teaching_group_id FROM attendance_record r JOIN course_session cs ON cs.id=r.course_session_id WHERE r.id=?",rs->rs.next()?(Long)rs.getObject(1):null,targetId);
        else throw new BusinessException("不支持审批目标："+table);
        if(groupId==null||!scope.contains(groupId))throw new BusinessException("只能审批本人教学分组的申请");
    }
    private int windowHours(){List<String> v=jdbcTemplate.queryForList("SELECT config_value FROM business_config WHERE config_key='score.approved_edit_window_hours'",String.class);try{return v.isEmpty()?24:Integer.parseInt(v.get(0));}catch(Exception e){return 24;}}
    private void insertAction(Long requestId,String action,String comment){Long uid=SecurityUtil.getCurrentUserId();String name=uid==null?"系统":jdbcTemplate.queryForObject("SELECT real_name FROM sys_user WHERE id=?",String.class,uid);jdbcTemplate.update("INSERT INTO approval_action(change_request_id,action,operator_user_id,operator_name_snapshot,comment_text) VALUES (?,?,?,?,?)",requestId,action,uid,name,comment);}
    private void expireWindows(){List<Long> ids=jdbcTemplate.queryForList("SELECT id FROM change_request WHERE status='APPROVED' AND edit_window_expires_at<CURRENT_TIMESTAMP(3)",Long.class);for(Long id:ids)if(jdbcTemplate.update("UPDATE change_request SET status='EXPIRED' WHERE id=? AND status='APPROVED'",id)==1)insertSystemAction(id);}
    private void insertSystemAction(Long id){jdbcTemplate.update("INSERT INTO approval_action(change_request_id,action,operator_name_snapshot,comment_text) VALUES (?,'EXPIRE','系统','审批修改窗口已过期')",id);}
    private String placeholders(int n){return String.join(",",java.util.Collections.nCopies(n,"?"));}
}
