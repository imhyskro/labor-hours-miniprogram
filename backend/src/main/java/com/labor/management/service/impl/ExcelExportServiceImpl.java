package com.labor.management.service.impl;

import com.labor.management.exception.BusinessException;
import com.labor.management.service.ExcelExportService;
import com.labor.management.vo.AssistantVO;
import com.labor.management.vo.AttendanceRecordVO;
import com.labor.management.vo.CertificateScoreVO;
import com.labor.management.vo.MasterListViewVO;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** Excel 导出服务实现。 */
@Service
public class ExcelExportServiceImpl implements ExcelExportService {

    @Override
    public byte[] exportStudents(List<MasterListViewVO> rows) {
        List<List<?>> data = new ArrayList<>();
        for (MasterListViewVO row : rows) {
            data.add(List.of(
                    value(row.getCompanyName()),
                    value(row.getFullNo()),
                    value(row.getStudentId()),
                    value(row.getName()),
                    genderText(row.getGender()),
                    value(row.getOriginalMajor())));
        }
        return build("学生", List.of("公司", "节次", "学号", "姓名", "性别", "行政班"), data);
    }

    @Override
    public byte[] exportAssistants(List<AssistantVO> rows) {
        List<List<?>> data = new ArrayList<>();
        for (AssistantVO row : rows) {
            data.add(List.of(
                    value(row.getCompanyName()),
                    value(row.getFullNo()),
                    value(row.getStudentId()),
                    value(row.getName()),
                    genderText(row.getGender()),
                    value(row.getOriginalMajor()),
                    value(row.getAssignedClassNames()),
                    row.getHasAccount() != null && row.getHasAccount() == 1 ? "是" : "否"));
        }
        return build("助教", List.of(
                "公司", "节次", "学号", "姓名", "性别", "行政班", "负责班级", "已有账号"), data);
    }

    @Override
    public byte[] exportAttendance(List<AttendanceRecordVO> rows) {
        List<List<?>> data = new ArrayList<>();
        for (AttendanceRecordVO row : rows) {
            data.add(List.of(
                    value(row.getStudentNoInClass()),
                    value(row.getStudentNo()),
                    value(row.getStudentName()),
                    attendanceTypeText(row.getAttendanceType()),
                    value(row.getScore()),
                    value(row.getRemark())));
        }
        return build("考勤", List.of("班内编号", "学号", "姓名", "考勤状态", "单次成绩", "备注"), data);
    }

    @Override
    public byte[] exportCertificateScores(List<CertificateScoreVO> rows) {
        List<List<?>> data = new ArrayList<>();
        for (CertificateScoreVO row : rows) {
            data.add(List.of(
                    value(row.getStudentNoInClass()),
                    value(row.getStudentNo()),
                    value(row.getStudentName()),
                    value(row.getAcademicYear()),
                    value(row.getSemester()),
                    value(row.getFinalScore()),
                    value(row.getRemark())));
        }
        return build("换证成绩", List.of(
                "班内编号", "学号", "姓名", "学年", "学期", "最终成绩", "备注"), data);
    }

    private byte[] build(String sheetName, List<String> headers, List<List<?>> data) {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet(sheetName);
            CellStyle headerStyle = createHeaderStyle(workbook);
            Row headerRow = sheet.createRow(0);
            for (int column = 0; column < headers.size(); column++) {
                Cell cell = headerRow.createCell(column);
                cell.setCellValue(headers.get(column));
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(column, 20 * 256);
            }

            for (int rowIndex = 0; rowIndex < data.size(); rowIndex++) {
                Row row = sheet.createRow(rowIndex + 1);
                List<?> values = data.get(rowIndex);
                for (int column = 0; column < values.size(); column++) {
                    setCellValue(row.createCell(column), values.get(column));
                }
            }
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new BusinessException("Excel 导出失败：" + e.getMessage());
        }
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    private void setCellValue(Cell cell, Object value) {
        if (value instanceof Number number) {
            cell.setCellValue(number.doubleValue());
        } else {
            cell.setCellValue(value == null ? "" : value.toString());
        }
    }

    private Object value(Object value) {
        return value == null ? "" : value;
    }

    private String genderText(Integer gender) {
        if (gender == null || gender == 0) return "未知";
        return gender == 1 ? "男" : gender == 2 ? "女" : "未知";
    }

    private String attendanceTypeText(String type) {
        if (type == null || type.isBlank()) return "未登记";
        return switch (type) {
            case "NORMAL" -> "正常";
            case "J" -> "请假";
            case "K" -> "旷课";
            default -> type;
        };
    }
}
