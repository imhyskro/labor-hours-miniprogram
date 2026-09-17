package com.labor.management.controller;

import com.labor.management.common.CommonResult;
import com.labor.management.dto.AttendanceRecordSaveDTO;
import com.labor.management.dto.AttendanceSessionCreateDTO;
import com.labor.management.dto.AttendanceSessionUpdateDTO;
import com.labor.management.service.AttendanceService;
import com.labor.management.service.ClassAccessService;
import com.labor.management.service.ClassesService;
import com.labor.management.service.ExcelExportService;
import com.labor.management.service.OperationLogService;
import com.labor.management.util.ExcelResponseUtil;
import com.labor.management.vo.AttendanceRecordVO;
import com.labor.management.vo.AttendanceSessionVO;
import com.labor.management.vo.ClassVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** 劳动课课次与考勤打分接口。 */
@RestController
@RequestMapping("/api/attendance")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('SUPER_ADMIN', 'TEACHER', 'ASSISTANT')")
public class AttendanceController {

    private final AttendanceService attendanceService;
    private final ClassesService classesService;
    private final ClassAccessService classAccessService;
    private final ExcelExportService excelExportService;
    private final OperationLogService operationLogService;

    /** 查询当前用户可访问的考勤班级。 */
    @GetMapping("/classes")
    public CommonResult<List<ClassVO>> listClasses() {
        return CommonResult.success(classesService.listAll(classAccessService.getAttendanceClassIds()));
    }

    /** 查询班级的全部劳动课课次。 */
    @GetMapping("/sessions")
    public CommonResult<List<AttendanceSessionVO>> listSessions(@RequestParam Long classId) {
        return CommonResult.success(attendanceService.listSessions(classId));
    }

    /** 新建课次；助教不能新建或修改周次。 */
    @PostMapping("/sessions")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN', 'TEACHER')")
    public CommonResult<Map<String, Long>> createSession(
            @Valid @RequestBody AttendanceSessionCreateDTO dto) {
        return CommonResult.success(Map.of("sessionId", attendanceService.createSession(dto)));
    }

    /** 修改课次日期、周次、最后一次课标志或封存状态；助教不可调用。 */
    @PutMapping("/sessions/{sessionId}")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN', 'TEACHER')")
    public CommonResult<Void> updateSession(@PathVariable Long sessionId,
                                            @Valid @RequestBody AttendanceSessionUpdateDTO dto) {
        attendanceService.updateSession(sessionId, dto);
        return CommonResult.success();
    }

    /** 查询某课次的全班学生及已登记的考勤、分数。 */
    @GetMapping("/sessions/{sessionId}/records")
    public CommonResult<List<AttendanceRecordVO>> listRecords(@PathVariable Long sessionId) {
        return CommonResult.success(attendanceService.listRecords(sessionId));
    }

    /** 导出某课次的全班考勤与单次成绩。 */
    @GetMapping("/sessions/{sessionId}/export")
    public ResponseEntity<byte[]> exportRecords(@PathVariable Long sessionId) {
        List<AttendanceRecordVO> rows = attendanceService.listRecords(sessionId);
        byte[] bytes = excelExportService.exportAttendance(rows);
        operationLogService.record("ATTENDANCE", "EXPORT", "ATTENDANCE_SESSION", sessionId,
                "导出考勤数据，共 " + rows.size() + " 条", null,
                Map.of("sessionId", sessionId, "count", rows.size()));
        return ExcelResponseUtil.download(bytes, "考勤数据-课次" + sessionId + ".xlsx");
    }

    /** 新增或覆盖单个学生的考勤与本次分数。 */
    @PutMapping("/sessions/{sessionId}/records/{studentId}")
    public CommonResult<Void> saveRecord(@PathVariable Long sessionId,
                                         @PathVariable Long studentId,
                                         @Valid @RequestBody AttendanceRecordSaveDTO dto) {
        attendanceService.saveRecord(sessionId, studentId, dto);
        return CommonResult.success();
    }

    /** 删除单个学生在该课次的考勤记录。 */
    @DeleteMapping("/sessions/{sessionId}/records/{studentId}")
    public CommonResult<Void> deleteRecord(@PathVariable Long sessionId,
                                           @PathVariable Long studentId) {
        attendanceService.deleteRecord(sessionId, studentId);
        return CommonResult.success();
    }
}
