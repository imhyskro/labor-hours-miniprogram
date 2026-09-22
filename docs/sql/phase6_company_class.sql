-- =====================================================================
-- 阶段6：公司 → 班级 → 学生 三级结构改造
--   公司(company) → 班级(classes: 公司+周次+节次段 W-S-E) → 学生(student: 班内编号N + 原始专业)
--   助教负责班级改为多对多(assistant_class)
-- 注意：本脚本会清空旧的班级/学生/助教账号业务数据（教师/管理员账号保留）
-- =====================================================================

USE labor_management;

-- 1. 清理旧业务数据 ------------------------------------------------
-- 删除导入创建的助教账号（关联了 student_id 的）及其角色关联
DELETE ur FROM user_role ur
  INNER JOIN sys_user u ON ur.user_id = u.id
  WHERE u.student_id IS NOT NULL;
DELETE FROM sys_user WHERE student_id IS NOT NULL;

-- 清空学生、班级（旧班级为公司粒度，结构已不符）
DELETE FROM student;
DELETE FROM classes;

-- 2. 公司表 --------------------------------------------------------
DROP TABLE IF EXISTS company;
CREATE TABLE company (
    id          BIGINT       NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    name        VARCHAR(100) NOT NULL                COMMENT '公司名称（茶园、果园、待定1...）',
    sort_order  INT          NOT NULL DEFAULT 0      COMMENT '排序号',
    status      TINYINT      NOT NULL DEFAULT 1      COMMENT '状态: 1=启用, 0=停用',
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP                                 COMMENT '创建时间',
    updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP   COMMENT '更新时间',
    deleted     TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除: 0=未删除, 1=已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_company_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='公司表';

-- 预置 8 个公司：茶园、果园 + 待定1~6
INSERT INTO company (name, sort_order) VALUES
  ('茶园', 1),
  ('果园', 2),
  ('待定1', 3),
  ('待定2', 4),
  ('待定3', 5),
  ('待定4', 6),
  ('待定5', 7),
  ('待定6', 8);

-- 3. 班级表改造：班级 = 公司 + 周次 + 开始节次 + 结束节次 -----------
ALTER TABLE classes
  ADD COLUMN company_id    BIGINT NULL COMMENT '所属公司ID' AFTER class_code,
  ADD COLUMN week          INT    NULL COMMENT '周次（第几周）' AFTER company_id,
  ADD COLUMN start_session INT    NULL COMMENT '开始节次' AFTER week,
  ADD COLUMN end_session   INT    NULL COMMENT '结束节次' AFTER start_session;

ALTER TABLE classes ADD KEY idx_classes_company (company_id);
-- class_code 语义改为自动生成的 "周次-开始节次-结束节次"（如 1-1-2）
ALTER TABLE classes MODIFY COLUMN class_code VARCHAR(50) NULL COMMENT '班级编码，格式：周次-开始节次-结束节次，如1-1-2';

-- 4. 学生表改造：加原始专业、班内编号；废弃单值负责班级字段 ----------
ALTER TABLE student
  ADD COLUMN original_major     VARCHAR(100) NULL COMMENT '原始专业（学生本来的专业）' AFTER gender,
  ADD COLUMN student_no_in_class INT        NULL COMMENT '班级内编号（完整编号 W-S-E-N 中的 N）' AFTER original_major;

-- 助教负责班级改为多对多，移除单值字段
ALTER TABLE student DROP COLUMN assigned_class_id;

ALTER TABLE student ADD KEY idx_student_no_in_class (class_id, student_no_in_class);
-- class_id 允许为空（助教可能非本学期学生）
ALTER TABLE student MODIFY COLUMN class_id BIGINT NULL COMMENT '所属班级ID';

-- 5. 助教-班级 多对多关联表 ----------------------------------------
DROP TABLE IF EXISTS assistant_class;
CREATE TABLE assistant_class (
    id                   BIGINT   NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    assistant_student_id BIGINT   NOT NULL                COMMENT '助教（student表主键）',
    class_id             BIGINT   NOT NULL                COMMENT '负责班级ID',
    created_at           DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP                                 COMMENT '创建时间',
    updated_at           DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP   COMMENT '更新时间',
    deleted              TINYINT  NOT NULL DEFAULT 0      COMMENT '逻辑删除: 0=未删除, 1=已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_assistant_class (assistant_student_id, class_id),
    KEY idx_assistant_class_class (class_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='助教负责班级关联表（多对多）';
