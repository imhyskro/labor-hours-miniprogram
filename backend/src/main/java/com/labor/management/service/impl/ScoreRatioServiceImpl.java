package com.labor.management.service.impl;

import com.labor.management.dto.RatioSettingDTO;
import com.labor.management.entity.ScoreRatioSetting;
import com.labor.management.exception.BusinessException;
import com.labor.management.service.ScoreRatioService;
import com.labor.management.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class ScoreRatioServiceImpl implements ScoreRatioService {
    private static final String PREFIX="web.score.ratio.";
    private final JdbcTemplate jdbcTemplate;
    @Override public ScoreRatioSetting getRatio(){ScoreRatioSetting r=new ScoreRatioSetting();r.setId(1L);r.setAttendanceRatio(read("attendance"));r.setCourseReportRatio(read("course_report"));r.setTheoryRatio(read("theory"));r.setPracticeRatio(read("practice"));return r;}
    @Override @Transactional public void updateRatio(RatioSettingDTO d){
        if(d.getAttendanceRatio()==null||d.getCourseReportRatio()==null||d.getTheoryRatio()==null||d.getPracticeRatio()==null)throw new BusinessException("4 个比例均不能为空");
        check(d.getAttendanceRatio());check(d.getCourseReportRatio());check(d.getTheoryRatio());check(d.getPracticeRatio());
        BigDecimal sum=d.getAttendanceRatio().add(d.getCourseReportRatio()).add(d.getTheoryRatio()).add(d.getPracticeRatio());
        if(sum.subtract(BigDecimal.ONE).abs().compareTo(new BigDecimal("0.0001"))>0)throw new BusinessException("4 个比例之和必须为 1");
        write("attendance",d.getAttendanceRatio());write("course_report",d.getCourseReportRatio());write("theory",d.getTheoryRatio());write("practice",d.getPracticeRatio());
    }
    private BigDecimal read(String key){java.util.List<String> values=jdbcTemplate.queryForList("SELECT config_value FROM business_config WHERE config_key=?",String.class,PREFIX+key);return values.isEmpty()?new BigDecimal("0.2500"):new BigDecimal(values.get(0));}
    private void write(String key,BigDecimal value){jdbcTemplate.update("""
            INSERT INTO business_config(config_key,config_value,value_type,description,updated_by_user_id)
            VALUES (?,?,'DECIMAL','Web端成绩权重兼容配置',?)
            ON DUPLICATE KEY UPDATE config_value=VALUES(config_value),updated_by_user_id=VALUES(updated_by_user_id)
            """,PREFIX+key,value.toPlainString(), SecurityUtil.getCurrentUserId());}
    private void check(BigDecimal v){if(v.compareTo(BigDecimal.ZERO)<0||v.compareTo(BigDecimal.ONE)>0)throw new BusinessException("比例必须在 0~1 之间");}
}
