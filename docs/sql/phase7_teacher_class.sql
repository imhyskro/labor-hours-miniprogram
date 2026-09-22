-- =====================================================================
-- 阶段7：教师-班级 多对多关系 + 修复 sys_user.student_id 字段
--
--   1. 修复 sys_user.student_id 字段（仅老库需要补，新库已含）
--      若同事用旧版 init_schema.sql 初始化，sys_user 缺 student_id 字段，
--      会导致登录时 MyBatis 查询列报错返回 401。本段通过 INFORMATION_SCHEMA
--      判断字段是否已存在，不存在则自动补加，已存在则跳过。
--
--   2. 新建 teacher_class 关联表（教师 ↔ 班级 多对多）
--      一个教师可负责多个班级，一个班级也可由多个教师负责。
--      老师访问 /api/classes、/api/students、/api/master-list 时
--      只能查看/操作其负责班级范围内的数据。
--
--   注意：本脚本可重复执行（幂等），不会破坏已有数据。
-- =====================================================================

USE labor_management;

-- ---------------------------------------------------------------------
-- 1. 修复 sys_user.student_id 字段（兼容老库）
-- ---------------------------------------------------------------------
SET @col_exists := (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = 'labor_management'
      AND TABLE_NAME = 'sys_user'
      AND COLUMN_NAME = 'student_id'
);
SET @ddl := IF(@col_exists = 0,
    'ALTER TABLE sys_user ADD COLUMN student_id BIGINT DEFAULT NULL COMMENT ''关联学生ID（助教账号关联 student 表主键，普通教师/管理员为空）'' AFTER last_password_change_time',
    'SELECT ''sys_user.student_id 字段已存在，跳过'' AS msg');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ---------------------------------------------------------------------
-- 2. 教师-班级 关联表（多对多）
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS teacher_class;
CREATE TABLE teacher_class (
    id          BIGINT   NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    user_id     BIGINT   NOT NULL                COMMENT '教师 sys_user 主键',
    class_id    BIGINT   NOT NULL                COMMENT '负责班级ID',
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP                                COMMENT '创建时间',
    updated_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP    COMMENT '更新时间',
    deleted     TINYINT  NOT NULL DEFAULT 0      COMMENT '逻辑删除: 0=未删除, 1=已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_teacher_class (user_id, class_id),
    KEY idx_teacher_class_class (class_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='教师负责班级关联表（多对多）';
