package com.labor.management.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.labor.management.entity.ScoreSetting;
import org.apache.ibatis.annotations.Mapper;

/**
 * 打分设置 Mapper（单行表）
 */
@Mapper
public interface ScoreSettingMapper extends BaseMapper<ScoreSetting> {
}
