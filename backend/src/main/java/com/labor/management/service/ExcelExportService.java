package com.labor.management.service;

import com.labor.management.vo.AssistantVO;
import com.labor.management.vo.AttendanceRecordVO;
import com.labor.management.vo.CertificateScoreVO;
import com.labor.management.vo.MasterListViewVO;

import java.util.List;

/** 学生、助教、考勤和成绩 Excel 导出服务。 */
public interface ExcelExportService {

    byte[] exportStudents(List<MasterListViewVO> rows);

    byte[] exportAssistants(List<AssistantVO> rows);

    byte[] exportAttendance(List<AttendanceRecordVO> rows);

    byte[] exportCertificateScores(List<CertificateScoreVO> rows);
}
