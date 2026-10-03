-- ============================================================
-- Phase 10: 成绩管理板块（课程报告 / 理论学习 / 项目实践 / 成绩汇总）
-- ============================================================
-- 说明：
--   1. course_report_score / theory_score / practice_score 三张表结构一致，
--      每个学生一个总分（0~100），唯一键 uk_student 不含 deleted（需复活逻辑，仿 student 表）。
--   2. attendance_score 为考勤分明细（多次课，每次 0~10），由小程序端写入；
--      本次仅建表 + 成绩汇总读取，不实现小程序端。
--   3. score_ratio_setting 为比例设置单行表（固定 id=1），4 个比例之和必须 = 1。
--   4. 复用现有 score_setting 表的 session_count 作为考勤分归一化分母。
-- ============================================================

-- 1. 课程报告分数
CREATE TABLE IF NOT EXISTS course_report_score (
  id          BIGINT       AUTO_INCREMENT PRIMARY KEY,
  student_id  BIGINT       NOT NULL COMMENT '关联 student.id',
  score       DECIMAL(6,2) NOT NULL COMMENT '分数 0~100',
  created_at  DATETIME     DEFAULT CURRENT_TIMESTAMP,
  updated_at  DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted     TINYINT      DEFAULT 0 COMMENT '0=未删 1=已删',
  UNIQUE KEY uk_student (student_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课程报告分数';

-- 2. 理论学习分数
CREATE TABLE IF NOT EXISTS theory_score (
  id          BIGINT       AUTO_INCREMENT PRIMARY KEY,
  student_id  BIGINT       NOT NULL COMMENT '关联 student.id',
  score       DECIMAL(6,2) NOT NULL COMMENT '分数 0~100',
  created_at  DATETIME     DEFAULT CURRENT_TIMESTAMP,
  updated_at  DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted     TINYINT      DEFAULT 0 COMMENT '0=未删 1=已删',
  UNIQUE KEY uk_student (student_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='理论学习分数';

-- 3. 项目实践分数
CREATE TABLE IF NOT EXISTS practice_score (
  id          BIGINT       AUTO_INCREMENT PRIMARY KEY,
  student_id  BIGINT       NOT NULL COMMENT '关联 student.id',
  score       DECIMAL(6,2) NOT NULL COMMENT '分数 0~100',
  created_at  DATETIME     DEFAULT CURRENT_TIMESTAMP,
  updated_at  DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted     TINYINT      DEFAULT 0 COMMENT '0=未删 1=已删',
  UNIQUE KEY uk_student (student_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='项目实践分数';

-- 4. 考勤分明细（小程序端写入）
CREATE TABLE IF NOT EXISTS attendance_score (
  id          BIGINT       AUTO_INCREMENT PRIMARY KEY,
  student_id  BIGINT       NOT NULL COMMENT '关联 student.id',
  session_no  INT          NOT NULL COMMENT '第几次课（1 起）',
  score       DECIMAL(4,2) NOT NULL COMMENT '本次课分数 0~10',
  created_at  DATETIME     DEFAULT CURRENT_TIMESTAMP,
  updated_at  DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted     TINYINT      DEFAULT 0 COMMENT '0=未删 1=已删',
  UNIQUE KEY uk_student_session (student_id, session_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='考勤分明细（小程序端同步）';

-- 5. 比例设置单行表（固定 id=1）
CREATE TABLE IF NOT EXISTS score_ratio_setting (
  id                   BIGINT       PRIMARY KEY DEFAULT 1,
  attendance_ratio     DECIMAL(5,4) DEFAULT 0.2500 COMMENT '考勤分比例',
  course_report_ratio  DECIMAL(5,4) DEFAULT 0.2500 COMMENT '课程报告比例',
  theory_ratio         DECIMAL(5,4) DEFAULT 0.2500 COMMENT '理论学习比例',
  practice_ratio       DECIMAL(5,4) DEFAULT 0.2500 COMMENT '项目实践比例',
  created_at           DATETIME     DEFAULT CURRENT_TIMESTAMP,
  updated_at           DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT chk_ratio_sum CHECK (
    ABS(attendance_ratio + course_report_ratio + theory_ratio + practice_ratio - 1.0) < 0.0001
  )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='成绩比例设置（单行）';

-- 初始化默认比例行（4 项各 25%）
INSERT INTO score_ratio_setting (id) VALUES (1)
  ON DUPLICATE KEY UPDATE id = id;
