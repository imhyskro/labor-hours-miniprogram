package com.labor.management.service;

import java.util.List;
import java.util.Map;

/**
 * 考勤分 Service（小程序端接口预留）
 *
 * <p>考勤分来源：小程序端助教打分，按 学生 × 第几次课 记录，每次 0~10 分。
 * 本次<b>不实现小程序端写入</b>，仅预留接口 + 成绩汇总读取能力。</p>
 * <p>预留接口：
 * <ul>
 *   <li>POST /api/attendance/sync —— 小程序端批量同步考勤分</li>
 *   <li>GET /api/attendance/student/{studentId} —— 查某学生所有考勤分</li>
 *   <li>GET /api/attendance/class/{classId} —— 查某班所有学生考勤分</li>
 * </ul>
 * 本次这些接口先返回空数据或"待小程序端同步"提示，待小程序端开发时再实现写入。</p>
 */
public interface AttendanceService {

    /**
     * 批量查询多个学生的考勤分汇总（成绩汇总用）
     *
     * <p>按学生分组求考勤分总和，归一化由调用方按 session_count 计算。</p>
     *
     * @param studentIds 学生ID列表
     * @return Map: studentId → 总分（各次课分数之和）
     */
    Map<Long, java.math.BigDecimal> sumByStudentIds(List<Long> studentIds);

    /**
     * 查询当前上课次数（归一化分母用，复用 score_setting 表 session_count）
     *
     * @return session_count，不存在则返回 0
     */
    int getSessionCount();
}
