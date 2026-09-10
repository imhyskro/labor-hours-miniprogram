package com.labor.management.controller;

import com.labor.management.common.CommonResult;
import com.labor.management.service.ExcelImportService;
import com.labor.management.vo.ImportResultVO;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Excel 导入 Controller
 *
 * <p>所有接口暂不加 @PreAuthorize 权限控制（JWT 认证后即可访问）。</p>
 */
@Slf4j
@RestController
@RequestMapping("/api/import")
@RequiredArgsConstructor
public class ImportController {

    private final ExcelImportService excelImportService;

    /** 导入学生 */
    @PostMapping("/students")
    public CommonResult<ImportResultVO> importStudents(@RequestParam("file") MultipartFile file) {
        return CommonResult.success(excelImportService.importStudents(file));
    }

    /** 导入助教 */
    @PostMapping("/assistants")
    public CommonResult<ImportResultVO> importAssistants(@RequestParam("file") MultipartFile file) {
        return CommonResult.success(excelImportService.importAssistants(file));
    }

    /** 下载学生导入模板 */
    @GetMapping("/template/students")
    public ResponseEntity<byte[]> downloadStudentTemplate() throws IOException {
        byte[] bytes = buildTemplate(
                "学生导入模板",
                new String[]{"学号", "姓名", "班级名称", "性别(男/女)"},
                new String[]{"S2024001", "张三", "果园", "男"}
        );
        return buildTemplateResponse(bytes, "学生导入模板.xlsx");
    }

    /** 下载助教导入模板 */
    @GetMapping("/template/assistants")
    public ResponseEntity<byte[]> downloadAssistantTemplate() throws IOException {
        byte[] bytes = buildTemplate(
                "助教导入模板",
                new String[]{"学号", "姓名", "所属班级(可空)"},
                new String[]{"S2024999", "王助教", "果园"}
        );
        return buildTemplateResponse(bytes, "助教导入模板.xlsx");
    }

    // ============== 模板构造 ==============

    /**
     * 构造模板：第1行表头(加粗+黄色背景)，第2行示例数据
     */
    private byte[] buildTemplate(String sheetName, String[] headers, String[] sampleRow) throws IOException {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet(sheetName);
            // 表头样式
            CellStyle headerStyle = workbook.createCellStyle();
            Font font = workbook.createFont();
            font.setBold(true);
            headerStyle.setFont(font);
            // 表头行
            Row header = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, 24 * 256);
            }
            // 示例行
            Row sample = sheet.createRow(1);
            for (int i = 0; i < sampleRow.length; i++) {
                sample.createCell(i).setCellValue(sampleRow[i]);
            }
            workbook.write(out);
            return out.toByteArray();
        }
    }

    /**
     * 构造下载响应（适配中文文件名）
     */
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
