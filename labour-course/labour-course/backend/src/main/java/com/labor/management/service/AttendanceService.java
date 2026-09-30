package com.labor.management.service;

import com.labor.management.dto.AttendanceBatchScoreDTO;
import com.labor.management.dto.AttendanceRecordSaveDTO;
import com.labor.management.dto.AttendanceSessionCreateDTO;
import com.labor.management.dto.AttendanceSessionUpdateDTO;
import com.labor.management.vo.AttendanceRecordVO;
import com.labor.management.vo.AttendanceSessionVO;

import java.util.List;

/** 劳动课课次及考勤记录服务。 */
public interface AttendanceService {

    List<AttendanceSessionVO> listSessions(Long classId);

    Long createSession(AttendanceSessionCreateDTO dto);

    void updateSession(Long sessionId, AttendanceSessionUpdateDTO dto);

    List<AttendanceRecordVO> listRecords(Long sessionId);

    void saveRecord(Long sessionId, Long studentId, AttendanceRecordSaveDTO dto);

    /** 一键打分：为一批学生批量设置相同的考勤类型与分数，存在则更新、不存在则插入。 */
    int batchScore(Long sessionId, AttendanceBatchScoreDTO dto);

    void deleteRecord(Long sessionId, Long studentId);
}
