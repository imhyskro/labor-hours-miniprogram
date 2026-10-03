package com.labor.management.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.labor.management.entity.ScoreRatioSetting;
import org.apache.ibatis.annotations.Mapper;

/**
 * 成绩比例设置 Mapper（单行表，固定 id=1）
 *
 * <p>4 个比例之和必须 = 1（数据库 CHECK 约束 + Service 二次校验）。</p>
 */
@Mapper
public interface ScoreRatioSettingMapper extends BaseMapper<ScoreRatioSetting> {
}
