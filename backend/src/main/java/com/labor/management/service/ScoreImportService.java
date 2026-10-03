package com.labor.management.service;

import com.labor.management.dto.ScoreSingleSaveDTO;
import com.labor.management.enums.ScoreType;
import com.labor.management.vo.ScoreImportResultVO;
import org.springframework.web.multipart.MultipartFile;

/**
 * 成绩导入 Service
 *
 * <p>课程报告 / 理论学习 / 项目实践 三个板块复用同一导入逻辑。
 * 导入规则：事务性整批校验，任一行错误 → 整批拒绝，全部通过才一起写入。
 * 错误明确指出预期值 vs 实际值，便于人工核查。</p>
 */
public interface ScoreImportService {

    /**
     * 事务性批量导入分数
     *
     * @param type 成绩板块类型（COURSE_REPORT / THEORY / PRACTICE）
     * @param file Excel 文件（4 列：学号 / 姓名 / 班级 / 分数）
     * @return 导入结果（含错误详情列表）
     */
    ScoreImportResultVO importScores(ScoreType type, MultipartFile file);

    /**
     * 单条新增 / 更新分数（手动补分）
     *
     * <p>学号已存在（含已删记录）→ 复活更新；不存在 → 新建。
     * 分数范围 0~100。</p>
     *
     * @param type 成绩板块类型
     * @param dto  学生ID + 分数
     */
    void saveSingle(ScoreType type, ScoreSingleSaveDTO dto);
}
