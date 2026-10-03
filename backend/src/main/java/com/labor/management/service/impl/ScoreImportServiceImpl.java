package com.labor.management.service.impl;

import com.labor.management.dto.ScoreSingleSaveDTO;
import com.labor.management.enums.ScoreType;
import com.labor.management.exception.BusinessException;
import com.labor.management.service.ScoreImportService;
import com.labor.management.vo.ScoreImportResultVO;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ScoreImportServiceImpl implements ScoreImportService {
    private BusinessException unsupported(){return new BusinessException("V2 数据库未定义课程报告、理论学习和项目实践独立成绩表；请先确定这些成绩在 V2 中的业务模型");}
    @Override public ScoreImportResultVO importScores(ScoreType type,MultipartFile file){throw unsupported();}
    @Override public void saveSingle(ScoreType type,ScoreSingleSaveDTO dto){throw unsupported();}
}
