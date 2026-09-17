package com.labor.management.controller;

import com.labor.management.common.CommonResult;
import com.labor.management.dto.CertificateScoreSaveDTO;
import com.labor.management.service.CertificateScoreService;
import com.labor.management.service.ExcelExportService;
import com.labor.management.service.OperationLogService;
import com.labor.management.util.ExcelResponseUtil;
import com.labor.management.vo.CertificateScoreVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** 换证考试最终成绩接口。 */
@RestController
@RequestMapping("/api/certificate-scores")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('SUPER_ADMIN', 'TEACHER')")
public class CertificateScoreController {

    private final CertificateScoreService certificateScoreService;
    private final ExcelExportService excelExportService;
    private final OperationLogService operationLogService;

    /** 查询班级全部学生在指定学年、学期的换证成绩。 */
    @GetMapping
    public CommonResult<List<CertificateScoreVO>> listScores(
            @RequestParam Long classId,
            @RequestParam String academicYear,
            @RequestParam Integer semester) {
        return CommonResult.success(
                certificateScoreService.listScores(classId, academicYear, semester));
    }

    /** 导出班级指定学年、学期的换证成绩。 */
    @GetMapping("/export")
    public ResponseEntity<byte[]> export(
            @RequestParam Long classId,
            @RequestParam String academicYear,
            @RequestParam Integer semester) {
        List<CertificateScoreVO> rows = certificateScoreService
                .listScores(classId, academicYear, semester);
        byte[] bytes = excelExportService.exportCertificateScores(rows);
        operationLogService.record("CERTIFICATE_SCORE", "EXPORT", "CLASS", classId,
                "导出换证成绩，共 " + rows.size() + " 条", null,
                Map.of("classId", classId, "academicYear", academicYear,
                        "semester", semester, "count", rows.size()));
        return ExcelResponseUtil.download(bytes, "换证成绩.xlsx");
    }

    /** 新增或覆盖单个学生的换证成绩。 */
    @PutMapping("/{studentId}")
    public CommonResult<Void> saveScore(@PathVariable Long studentId,
                                        @Valid @RequestBody CertificateScoreSaveDTO dto) {
        certificateScoreService.saveScore(studentId, dto);
        return CommonResult.success();
    }

    /** 删除单个学生指定学年、学期的换证成绩。 */
    @DeleteMapping("/{studentId}")
    public CommonResult<Void> deleteScore(
            @PathVariable Long studentId,
            @RequestParam Long classId,
            @RequestParam String academicYear,
            @RequestParam Integer semester) {
        certificateScoreService.deleteScore(studentId, classId, academicYear, semester);
        return CommonResult.success();
    }
}
