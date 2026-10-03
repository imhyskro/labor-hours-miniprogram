package com.labor.management.controller;

import com.labor.management.common.CommonResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/attendance")
@RequiredArgsConstructor
@Tag(name="考勤管理",description="读取 V2 attendance_record 和 score_record")
public class AttendanceController {
    private final JdbcTemplate jdbcTemplate;

    @Operation(summary="V2 共库模式无需同步")
    @PostMapping("/sync")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    public CommonResult<Map<String,String>> sync(){return CommonResult.success(Map.of("message","Web端与小程序端共用V2数据库，无需复制同步"));}

    @Operation(summary="查询学生课次考勤与分数")
    @GetMapping("/student/{studentId}")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','TEACHER')")
    public CommonResult<List<Object>> getByStudent(@PathVariable Long studentId){
        List<Object> rows=jdbcTemplate.query("""
                SELECT cs.id sessionId,cs.session_date sessionDate,cs.start_period startPeriod,cs.end_period endPeriod,
                       ar.attendance_type attendanceType,sr.score_value scoreValue,sr.score_mark scoreMark,sr.locked
                FROM student_course_enrollment e JOIN course_session cs ON cs.teaching_group_id=e.teaching_group_id
                LEFT JOIN attendance_record ar ON ar.course_session_id=cs.id AND ar.enrollment_id=e.id
                LEFT JOIN score_record sr ON sr.course_session_id=cs.id AND sr.enrollment_id=e.id
                WHERE e.student_id=? ORDER BY cs.session_date,cs.start_period
                """,(rs,n)->(Object)Map.of("sessionId",rs.getLong("sessionId"),"sessionDate",rs.getDate("sessionDate").toLocalDate(),
                "startPeriod",rs.getInt("startPeriod"),"endPeriod",rs.getInt("endPeriod"),
                "attendanceType",String.valueOf(rs.getObject("attendanceType")),"score",String.valueOf(rs.getObject("scoreValue")),
                "scoreMark",String.valueOf(rs.getObject("scoreMark")),"locked",rs.getBoolean("locked")),studentId);
        return CommonResult.success(rows);
    }

    @Operation(summary="查询教学班考勤与分数")
    @GetMapping("/class/{classId}")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','TEACHER')")
    public CommonResult<List<Object>> getByClass(@PathVariable Long classId){
        List<Object> rows=jdbcTemplate.query("""
                SELECT e.student_id studentId,e.student_no_snapshot studentNo,e.student_name_snapshot studentName,
                       cs.id sessionId,cs.session_date sessionDate,ar.attendance_type attendanceType,
                       sr.score_value scoreValue,sr.score_mark scoreMark,sr.locked
                FROM student_course_enrollment e JOIN course_session cs ON cs.teaching_group_id=e.teaching_group_id
                LEFT JOIN attendance_record ar ON ar.course_session_id=cs.id AND ar.enrollment_id=e.id
                LEFT JOIN score_record sr ON sr.course_session_id=cs.id AND sr.enrollment_id=e.id
                WHERE e.teaching_group_id=? AND e.status<>'WITHDRAWN'
                ORDER BY e.in_class_no,cs.session_date,cs.start_period
                """,(rs,n)->(Object)Map.of("studentId",rs.getLong("studentId"),"studentNo",rs.getString("studentNo"),
                "studentName",rs.getString("studentName"),"sessionId",rs.getLong("sessionId"),
                "sessionDate",rs.getDate("sessionDate").toLocalDate(),"attendanceType",String.valueOf(rs.getObject("attendanceType")),
                "score",String.valueOf(rs.getObject("scoreValue")),"scoreMark",String.valueOf(rs.getObject("scoreMark")),"locked",rs.getBoolean("locked")),classId);
        return CommonResult.success(rows);
    }
}
