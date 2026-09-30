package com.labor.management.controller;

import com.labor.management.common.CommonResult;
import com.labor.management.dto.ApprovalDecisionDTO;
import com.labor.management.dto.AttendanceBatchScoreDTO;
import com.labor.management.dto.AttendanceRecordSaveDTO;
import com.labor.management.dto.ModificationRequestCreateDTO;
import com.labor.management.service.ExcelExportService;
import com.labor.management.service.V2AttendanceService;
import com.labor.management.util.ExcelResponseUtil;
import com.labor.management.vo.ApprovalActionVO;
import com.labor.management.vo.AttendanceRecordVO;
import com.labor.management.vo.AttendanceSessionVO;
import com.labor.management.vo.ChangeRequestVO;
import com.labor.management.vo.ClassVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** V2 劳动课课次、考勤打分和修改审批接口。 */
@RestController
@RequestMapping("/api/attendance")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('ADMIN', 'SUPER_ADMIN', 'TEACHER', 'ASSISTANT')")
public class AttendanceController {

    private final V2AttendanceService attendanceService;
    private final ExcelExportService excelExportService;

    /** 查询当前用户可访问的教学分组。返回 id 即 teaching_group.id。 */
    @GetMapping("/classes")
    public CommonResult<List<ClassVO>> listClasses() {
        return CommonResult.success(attendanceService.listAccessibleGroups());
    }

    /** 查询教学分组的课次；课次由迁移/课表产生，小程序端不再提供“新建”。 */
    @GetMapping("/sessions")
    public CommonResult<List<AttendanceSessionVO>> listSessions(@RequestParam Long classId) {
        return CommonResult.success(attendanceService.listSessions(classId));
    }

    /** 查询某课次的修读学生、考勤和当前成绩。 */
    @GetMapping("/sessions/{sessionId}/records")
    public CommonResult<List<AttendanceRecordVO>> listRecords(@PathVariable Long sessionId) {
        return CommonResult.success(attendanceService.listRecords(sessionId));
    }

    /** 导出某课次当前考勤和成绩。 */
    @GetMapping("/sessions/{sessionId}/export")
    public ResponseEntity<byte[]> exportRecords(@PathVariable Long sessionId) {
        List<AttendanceRecordVO> rows = attendanceService.listRecords(sessionId);
        return ExcelResponseUtil.download(excelExportService.exportAttendance(rows),
                "考勤数据-课次" + sessionId + ".xlsx");
    }

    /** 首次录入、直接首次修改或持已通过申请修改。 */
    @PutMapping("/sessions/{sessionId}/records/{studentId}")
    public CommonResult<Void> saveRecord(@PathVariable Long sessionId,
                                         @PathVariable Long studentId,
                                         @Valid @RequestBody AttendanceRecordSaveDTO dto) {
        attendanceService.saveRecord(sessionId, studentId, dto);
        return CommonResult.success();
    }

    /** 一键打分；已打分、助教身份或需要审批的学生会跳过。 */
    @PostMapping("/sessions/{sessionId}/batch-score")
    public CommonResult<Map<String, Integer>> batchScore(@PathVariable Long sessionId,
                                                         @Valid @RequestBody AttendanceBatchScoreDTO dto) {
        int success = attendanceService.batchScore(sessionId, dto);
        return CommonResult.success(Map.of(
                "success", success,
                "skipped", dto.getStudentIds().size() - success));
    }

    /** 助教/教师提交成绩修改申请。 */
    @PostMapping("/modification-requests")
    public CommonResult<Map<String, Long>> submitModificationRequest(
            @Valid @RequestBody ModificationRequestCreateDTO dto) {
        return CommonResult.success(Map.of(
                "requestId", attendanceService.submitModificationRequest(dto)));
    }

    /** Web审批列表；教师仅能查看本人教学分组，管理员查看全部。 */
    @GetMapping("/modification-requests")
    public CommonResult<List<ChangeRequestVO>> listModificationRequests(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long sessionId) {
        return CommonResult.success(attendanceService.listModificationRequests(status, sessionId));
    }

    /** 申请详情，包含课次、学生和当前成绩。 */
    @GetMapping("/modification-requests/{requestId}")
    public CommonResult<ChangeRequestVO> getModificationRequest(@PathVariable Long requestId) {
        return CommonResult.success(attendanceService.getModificationRequest(requestId));
    }

    /** 教师/管理员通过或驳回申请。 */
    @PutMapping("/modification-requests/{requestId}/decision")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'SUPER_ADMIN', 'TEACHER')")
    public CommonResult<Void> decideModificationRequest(
            @PathVariable Long requestId,
            @Valid @RequestBody ApprovalDecisionDTO dto) {
        attendanceService.decideModificationRequest(requestId, dto);
        return CommonResult.success();
    }

    /** 申请人在待审批阶段取消申请。 */
    @PutMapping("/modification-requests/{requestId}/cancel")
    public CommonResult<Void> cancelModificationRequest(@PathVariable Long requestId) {
        attendanceService.cancelModificationRequest(requestId);
        return CommonResult.success();
    }

    /** 查询提交、审批、使用、取消、过期等完整流水。 */
    @GetMapping("/modification-requests/{requestId}/actions")
    public CommonResult<List<ApprovalActionVO>> listApprovalActions(@PathVariable Long requestId) {
        return CommonResult.success(attendanceService.listActions(requestId));
    }
}
