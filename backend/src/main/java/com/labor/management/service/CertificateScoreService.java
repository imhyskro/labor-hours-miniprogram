package com.labor.management.service;

import com.labor.management.dto.CertificateScoreSaveDTO;
import com.labor.management.vo.CertificateScoreVO;

import java.util.List;

/** 换证考试成绩服务。 */
public interface CertificateScoreService {

    List<CertificateScoreVO> listScores(Long classId, String academicYear, Integer semester);

    void saveScore(Long studentId, CertificateScoreSaveDTO dto);

    void deleteScore(Long studentId, Long classId, String academicYear, Integer semester);
}
