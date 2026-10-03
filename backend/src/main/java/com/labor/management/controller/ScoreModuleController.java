package com.labor.management.controller;

import com.labor.management.common.CommonResult;
import com.labor.management.dto.ScoreSingleSaveDTO;
import com.labor.management.enums.ScoreType;
import com.labor.management.service.ScoreImportService;
import com.labor.management.vo.ScoreImportResultVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 成绩板块 Controller（课程报告 / 理论学习 / 项目实践 复用）
 *
 * <p>三个板块通过 @PathVariable type 区分（course-report / theory / practice），
 * 共用 ScoreImportService，减少重复代码。</p>
 * <p>权限：仅超级管理员可访问（4 个板块仅超管可见）。</p>
 */
@Slf4j
@RestController
@RequestMapping("/api/scores")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('SUPER_ADMIN')")
@Tag(name = "成绩板块管理", description = "课程报告/理论学习/项目实践成绩的Excel导入、单条补分与模板下载（仅超级管理员）")
public class ScoreModuleController {

    private final ScoreImportService scoreImportService;

    /**
     * Excel 批量导入分数（事务性：任一行错误 → 整批拒绝）
     */
    @Operation(summary = "Excel批量导入分数")
    @PostMapping("/{type}/import")
    public CommonResult<ScoreImportResultVO> importScores(
            @PathVariable String type,
            @RequestParam("file") MultipartFile file) {
        ScoreType scoreType = ScoreType.fromPath(type);
        return CommonResult.success(scoreImportService.importScores(scoreType, file));
    }

    /**
     * 单条新增 / 更新分数（手动补分）
     */
    @Operation(summary = "单条新增或更新分数（手动补分）")
    @PostMapping("/{type}/single")
    public CommonResult<Void> saveSingle(
            @PathVariable String type,
            @RequestBody ScoreSingleSaveDTO dto) {
        ScoreType scoreType = ScoreType.fromPath(type);
        scoreImportService.saveSingle(scoreType, dto);
        return CommonResult.success();
    }

    /**
     * 下载导入模板（4 列：学号 / 姓名 / 班级 / 分数）
     */
    @Operation(summary = "下载成绩导入模板")
    @GetMapping("/{type}/template")
    public ResponseEntity<byte[]> downloadTemplate(@PathVariable String type) throws IOException {
        // 校验 type 合法性
        ScoreType scoreType = ScoreType.fromPath(type);
        byte[] bytes = buildTemplate(scoreType.getLabel() + "导入模板");
        String fileName = scoreType.getLabel() + "导入模板.xlsx";
        return buildTemplateResponse(bytes, fileName);
    }

    // ============== 模板构造 ==============

    private byte[] buildTemplate(String sheetName) throws IOException {
        String[] headers = {"学号", "姓名", "班级", "分数"};
        String[] sampleRow = {"2024001001", "张三", "茶园-周一-1~2节", "85"};
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet(sheetName);
            CellStyle headerStyle = workbook.createCellStyle();
            Font font = workbook.createFont();
            font.setBold(true);
            headerStyle.setFont(font);
            Row header = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, 24 * 256);
            }
            Row sample = sheet.createRow(1);
            for (int i = 0; i < sampleRow.length; i++) {
                sample.createCell(i).setCellValue(sampleRow[i]);
            }
            workbook.write(out);
            return out.toByteArray();
        }
    }

    private ResponseEntity<byte[]> buildTemplateResponse(byte[] bytes, String fileName) {
        String encoded = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + encoded + "\"; filename*=UTF-8''" + encoded)
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .contentLength(bytes.length)
                .body(bytes);
    }
}
