package com.labor.management.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 成绩板块类型枚举
 *
 * <p>用于区分课程报告 / 理论学习 / 项目实践 三个成绩板块。
 * 考勤分（ATTENDANCE）由小程序端同步，不参与本导入流程。</p>
 */
@Getter
@AllArgsConstructor
public enum ScoreType {

    /** 课程报告 */
    COURSE_REPORT("course-report", "课程报告"),
    /** 理论学习 */
    THEORY("theory", "理论学习"),
    /** 项目实践 */
    PRACTICE("practice", "项目实践");

    /** URL 路径标识 */
    private final String path;
    /** 中文名称 */
    private final String label;

    /**
     * 按路径标识解析枚举（不区分大小写），无效则抛 BusinessException
     */
    public static ScoreType fromPath(String path) {
        if (path == null) {
            throw new IllegalArgumentException("成绩类型不能为空");
        }
        for (ScoreType t : values()) {
            if (t.path.equalsIgnoreCase(path)) {
                return t;
            }
        }
        throw new IllegalArgumentException("不支持的成绩类型：" + path);
    }
}
