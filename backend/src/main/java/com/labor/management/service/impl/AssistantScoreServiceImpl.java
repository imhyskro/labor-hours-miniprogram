package com.labor.management.service.impl;

import com.labor.management.dto.ScoreSaveDTO;
import com.labor.management.exception.BusinessException;
import com.labor.management.service.AssistantScoreService;
import com.labor.management.vo.AssistantScoreTableVO;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class AssistantScoreServiceImpl implements AssistantScoreService {
    private BusinessException unsupported(){return new BusinessException("V2 数据库不保存助教成绩；助教不属于 score_record 的被评分学生范围");}
    @Override public AssistantScoreTableVO getScoreTable(Integer page,Integer size,String keyword,List<Long> scopeClassIds){throw unsupported();}
    @Override public void saveScores(ScoreSaveDTO dto,List<Long> scopeClassIds){throw unsupported();}
    @Override public void updateSessionCount(Integer sessionCount){throw new BusinessException("V2 上课次数由 course_session 实际课次决定，不能手工设置");}
}
