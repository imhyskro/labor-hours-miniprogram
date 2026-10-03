package com.labor.management.service;

import com.labor.management.dto.RatioSettingDTO;
import com.labor.management.entity.ScoreRatioSetting;

/**
 * 成绩比例设置 Service
 *
 * <p>4 个比例之和必须 = 1（容差 0.0001），前端 + 后端双校验。</p>
 */
public interface ScoreRatioService {

    /**
     * 读取比例设置（单行表 id=1，不存在则建默认 0.25×4）
     */
    ScoreRatioSetting getRatio();

    /**
     * 更新比例设置（校验和=1 后更新）
     *
     * @param dto 4 个比例字段
     */
    void updateRatio(RatioSettingDTO dto);
}
