-- ============================================================================
-- 文件：00_rebuild_schema.sql
-- 作用：从空库开始建立劳动学时管理系统 V2 的完整数据库结构。
-- 适用：MySQL 8.0.16 及以上版本（使用了 CHECK、JSON、生成列等功能）。
--
-- 重要说明：
-- 1. 本脚本会删除并重新创建 labor_management_v2，库内原有数据会全部丢失。
-- 2. 本脚本不会修改旧库 labor_management_test，旧系统仍可继续连接旧库。
-- 3. 本脚本只建立表、索引、外键和约束，不创建测试账号和测试业务数据。
-- 4. 建库完成后应继续执行 01_seed_reference.sql。
-- 5. 现有 Java 后端使用旧表结构，完成后端迁移前不能直接切换到此数据库。
--
-- 设计原则：
-- - 人员主档与学期修读关系分开，避免每学期重复保存学生基本信息。
-- - 班级由“公司+学年学期+周次+连续课节”确定，例如“果园/周1-1-2”。
-- - “节次”是导入模板沿用的业务列名，最后一段实质是学生在该班内的编号。
-- - 课次表示上述班级在某个具体日期实际发生的一次课。
-- - 当前数据允许物理删除，关键历史表保存业务快照，避免历史记录失去含义。
-- - 导入、导出、审批、通知、日志均单独建模，便于后续扩展和审计。
-- ============================================================================
SET NAMES utf8mb4;
-- 建表期间临时关闭外键检查，避免删除旧 V2 库或创建表时受引用顺序影响。
SET FOREIGN_KEY_CHECKS = 0;
DROP DATABASE IF EXISTS labor_management_v2;
CREATE DATABASE labor_management_v2 CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE labor_management_v2;

-- ============================================================================
-- 一、学年学期与人员基础档案
-- ============================================================================

-- 学年学期：所有教学分组、修读关系、课次和导入批次的时间范围根节点。
-- active_guard 是生成列，通过唯一索引保证同一时间最多只有一个 ACTIVE 学期。
CREATE TABLE academic_term (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    academic_year VARCHAR(20) NOT NULL COMMENT '学年，如2026-2027',
    semester TINYINT UNSIGNED NOT NULL COMMENT '学期：1或2',
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    status ENUM('PLANNED','ACTIVE','ARCHIVED') NOT NULL DEFAULT 'PLANNED',
    archived_at DATETIME(3) NULL,
    active_guard TINYINT GENERATED ALWAYS AS
        (CASE WHEN status='ACTIVE' THEN 1 ELSE NULL END) STORED,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_term_year_semester (academic_year, semester),
    UNIQUE KEY uk_only_one_active_term (active_guard),
    CONSTRAINT ck_term_semester CHECK (semester IN (1,2)),
    CONSTRAINT ck_term_date CHECK (end_date >= start_date)
) ENGINE=InnoDB COMMENT='学年学期；导入时由页面或接口指定';

-- 公司：业务上的“大班”，例如“果园”“桃园”“一公司”。
-- company_code 是内部稳定标识；页面仍可只要求用户填写公司名称，由后端生成编码。
CREATE TABLE company (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    company_code VARCHAR(50) NOT NULL COMMENT '稳定业务编码',
    company_name VARCHAR(100) NOT NULL COMMENT '公司/大班名称',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_company_code (company_code),
    UNIQUE KEY uk_company_name (company_name)
) ENGINE=InnoDB COMMENT='公司（大班）基础档案；历史表另存快照';

-- 行政班：学校原始行政班数据，不与劳动课程中的教学分组混为一张表。
CREATE TABLE administrative_class (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    administrative_class_name VARCHAR(100) NOT NULL COMMENT '原始行政班名称',
    admission_year SMALLINT UNSIGNED NULL,
    major_name VARCHAR(150) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_admin_class_name (administrative_class_name)
) ENGINE=InnoDB COMMENT='原始行政班；预留行政班画像与逻辑分班';

-- 学生主档：每名学生只保存一份稳定身份信息。
-- 学生本学期在哪个班、是不是重修，不保存在这里，而在 student_course_enrollment 中保存。
CREATE TABLE student (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    student_no VARCHAR(50) NOT NULL COMMENT '学号，全系统稳定标识',
    student_name VARCHAR(100) NOT NULL,
    gender ENUM('男','女','未知') NOT NULL DEFAULT '未知',
    administrative_class_id BIGINT UNSIGNED NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_student_no (student_no),
    KEY idx_student_name (student_name),
    KEY idx_student_admin_class (administrative_class_id),
    CONSTRAINT fk_student_admin_class FOREIGN KEY (administrative_class_id)
        REFERENCES administrative_class(id) ON DELETE SET NULL ON UPDATE RESTRICT,
    CONSTRAINT ck_student_no_not_blank CHECK (CHAR_LENGTH(TRIM(student_no)) > 0),
    CONSTRAINT ck_student_name_not_blank CHECK (CHAR_LENGTH(TRIM(student_name)) > 0)
) ENGINE=InnoDB COMMENT='学生主档；不直接保存某学期班级关系';

-- ============================================================================
-- 二、账号与角色权限
-- ============================================================================

-- 角色字典：当前预置超级管理员、教师、助教三类角色。
CREATE TABLE sys_role (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    role_code VARCHAR(40) NOT NULL,
    role_name VARCHAR(60) NOT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_role_code (role_code)
) ENGINE=InnoDB COMMENT='系统角色';

-- 登录账号：学生本身没有登录账号；教师、助教、超级管理员才需要账号。
-- 新建账号时 last_login_at、last_password_change_at 均应为 NULL。
CREATE TABLE sys_user (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    username VARCHAR(80) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    real_name VARCHAR(100) NOT NULL,
    status ENUM('ACTIVE','DISABLED','LOCKED') NOT NULL DEFAULT 'ACTIVE',
    first_login BOOLEAN NOT NULL DEFAULT TRUE,
    last_login_at DATETIME(3) NULL COMMENT '真正登录成功后写入',
    last_password_change_at DATETIME(3) NULL COMMENT '真正修改密码后写入，新建时为空',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_username (username),
    KEY idx_user_status (status)
) ENGINE=InnoDB COMMENT='登录账号';

-- 用户与角色的多对多关系。当前通常一个用户一个角色，但结构允许未来扩展多角色。
CREATE TABLE user_role (
    user_id BIGINT UNSIGNED NOT NULL,
    role_id BIGINT UNSIGNED NOT NULL,
    granted_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_role_user FOREIGN KEY (user_id) REFERENCES sys_user(id)
        ON DELETE CASCADE ON UPDATE RESTRICT,
    CONSTRAINT fk_user_role_role FOREIGN KEY (role_id) REFERENCES sys_role(id)
        ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB COMMENT='用户角色关系';

-- ============================================================================
-- 三、教学分组以及教师、助教的分配关系
-- ============================================================================

-- 教学分组：对应业务上的具体班级，用于关联教师、助教、学生和课次。
-- 班级在某学期、某公司范围内由“周次+连续课节”唯一确定，例如“周1-1-2”。
-- class_identifier 是数据库自动生成的显示标识，不是额外要求业务人员维护的编码。
-- 一个分组只有一个 current_teacher_user_id；一个教师可以管理多个分组。
CREATE TABLE teaching_group (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    term_id BIGINT UNSIGNED NOT NULL,
    company_id BIGINT UNSIGNED NULL,
    company_code_snapshot VARCHAR(50) NOT NULL,
    company_name_snapshot VARCHAR(100) NOT NULL,
    week TINYINT UNSIGNED NOT NULL COMMENT '业务方称“周次”，取值1-5',
    start_period TINYINT UNSIGNED NOT NULL COMMENT '连续课节起始值，只能为1/3/5/7',
    end_period TINYINT UNSIGNED GENERATED ALWAYS AS (start_period + 1) STORED,
    class_identifier VARCHAR(30) GENERATED ALWAYS AS
        (CONCAT('周', week, '-', start_period, '-', start_period + 1)) STORED
        COMMENT '自动生成的班级唯一显示标识，如周1-1-2',
    current_teacher_user_id BIGINT UNSIGNED NULL COMMENT '当前唯一任课教师',
    status ENUM('PLANNED','ACTIVE','ARCHIVED') NOT NULL DEFAULT 'PLANNED',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_group_term_company_schedule
        (term_id, company_code_snapshot, week, start_period),
    KEY idx_group_term_status (term_id, status),
    KEY idx_group_teacher (current_teacher_user_id),
    CONSTRAINT ck_group_week CHECK (week BETWEEN 1 AND 5),
    CONSTRAINT ck_group_start_period CHECK (start_period IN (1,3,5,7)),
    CONSTRAINT fk_group_term FOREIGN KEY (term_id) REFERENCES academic_term(id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_group_company FOREIGN KEY (company_id) REFERENCES company(id)
        ON DELETE SET NULL ON UPDATE RESTRICT,
    CONSTRAINT fk_group_teacher FOREIGN KEY (current_teacher_user_id) REFERENCES sys_user(id)
        ON DELETE SET NULL ON UPDATE RESTRICT
) ENGINE=InnoDB COMMENT='班级关系；由公司、学期、周次和连续课节唯一确定';

-- 教师分配历史：教师更换时不覆盖历史，而是结束旧记录并增加新记录。
-- current_guard=1 表示当前分配；结束分配时同时写 unassigned_at 并把它改为 NULL。
-- 唯一索引利用 MySQL 允许多个 NULL 的规则，保证一个分组最多只有一条当前记录。
-- 不使用“依赖外键列的生成列”，避免与 ON DELETE SET NULL 的 MySQL 限制冲突。
CREATE TABLE teacher_assignment_history (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    teaching_group_id BIGINT UNSIGNED NULL,
    teacher_user_id BIGINT UNSIGNED NULL,
    teacher_username_snapshot VARCHAR(80) NOT NULL,
    teacher_name_snapshot VARCHAR(100) NOT NULL,
    assigned_at DATETIME(3) NOT NULL,
    unassigned_at DATETIME(3) NULL,
    current_guard TINYINT UNSIGNED NULL DEFAULT 1 COMMENT '当前记录为1，历史记录为NULL',
    assigned_by_user_id BIGINT UNSIGNED NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_teacher_history_group_time (teaching_group_id, assigned_at),
    KEY idx_teacher_history_teacher_time (teacher_user_id, assigned_at),
    UNIQUE KEY uk_one_current_teacher_history (teaching_group_id, current_guard),
    CONSTRAINT ck_teacher_assignment_time CHECK (unassigned_at IS NULL OR unassigned_at >= assigned_at),
    CONSTRAINT ck_teacher_current_guard CHECK (
        (unassigned_at IS NULL AND current_guard = 1)
        OR (unassigned_at IS NOT NULL AND current_guard IS NULL)
    ),
    CONSTRAINT fk_teacher_history_group FOREIGN KEY (teaching_group_id) REFERENCES teaching_group(id)
        ON DELETE SET NULL ON UPDATE RESTRICT,
    CONSTRAINT fk_teacher_history_teacher FOREIGN KEY (teacher_user_id) REFERENCES sys_user(id)
        ON DELETE SET NULL ON UPDATE RESTRICT,
    CONSTRAINT fk_teacher_history_operator FOREIGN KEY (assigned_by_user_id) REFERENCES sys_user(id)
        ON DELETE SET NULL ON UPDATE RESTRICT
) ENGINE=InnoDB COMMENT='任课教师分配历史';

-- 助教身份：助教首先是一名学生，再额外拥有助教身份和登录账号。
-- 快照字段用于学生主档或账号被物理删除后继续解释历史数据。
CREATE TABLE assistant_profile (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    student_id BIGINT UNSIGNED NULL,
    user_id BIGINT UNSIGNED NULL,
    student_no_snapshot VARCHAR(50) NOT NULL,
    student_name_snapshot VARCHAR(100) NOT NULL,
    gender_snapshot ENUM('男','女','未知') NOT NULL DEFAULT '未知',
    admin_class_snapshot VARCHAR(100) NOT NULL,
    status ENUM('ACTIVE','INACTIVE') NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_assistant_student_no (student_no_snapshot),
    UNIQUE KEY uk_assistant_user (user_id),
    CONSTRAINT fk_assistant_student FOREIGN KEY (student_id) REFERENCES student(id)
        ON DELETE SET NULL ON UPDATE RESTRICT,
    CONSTRAINT fk_assistant_user FOREIGN KEY (user_id) REFERENCES sys_user(id)
        ON DELETE SET NULL ON UPDATE RESTRICT
) ENGINE=InnoDB COMMENT='助教身份；六列基础信息复用学生主档';

-- 助教分组关系：一名助教可负责多个分组，一个分组也可分配多个助教。
-- current_guard=1 表示当前有效分配；取消分配时写 unassigned_at 并改为 NULL。
-- 复合唯一索引防止同一助教被重复分配到同一分组，同时允许保留多条历史记录。
CREATE TABLE assistant_group_assignment (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    assistant_profile_id BIGINT UNSIGNED NULL,
    teaching_group_id BIGINT UNSIGNED NULL,
    assistant_no_snapshot VARCHAR(50) NOT NULL,
    assistant_name_snapshot VARCHAR(100) NOT NULL,
    company_name_snapshot VARCHAR(100) NOT NULL,
    class_identifier_snapshot VARCHAR(30) NOT NULL COMMENT '班级标识快照，如周1-1-2',
    assigned_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    unassigned_at DATETIME(3) NULL,
    current_guard TINYINT UNSIGNED NULL DEFAULT 1 COMMENT '当前记录为1，历史记录为NULL',
    assigned_by_user_id BIGINT UNSIGNED NULL,
    PRIMARY KEY (id),
    KEY idx_assistant_assignment_profile (assistant_profile_id, unassigned_at),
    KEY idx_assistant_assignment_group (teaching_group_id, unassigned_at),
    UNIQUE KEY uk_active_assistant_group
        (assistant_profile_id, teaching_group_id, current_guard),
    CONSTRAINT ck_assistant_assignment_time CHECK (unassigned_at IS NULL OR unassigned_at >= assigned_at),
    CONSTRAINT ck_assistant_current_guard CHECK (
        (unassigned_at IS NULL AND current_guard = 1)
        OR (unassigned_at IS NOT NULL AND current_guard IS NULL)
    ),
    CONSTRAINT fk_assistant_assignment_profile FOREIGN KEY (assistant_profile_id) REFERENCES assistant_profile(id)
        ON DELETE SET NULL ON UPDATE RESTRICT,
    CONSTRAINT fk_assistant_assignment_group FOREIGN KEY (teaching_group_id) REFERENCES teaching_group(id)
        ON DELETE SET NULL ON UPDATE RESTRICT,
    CONSTRAINT fk_assistant_assignment_operator FOREIGN KEY (assigned_by_user_id) REFERENCES sys_user(id)
        ON DELETE SET NULL ON UPDATE RESTRICT
) ENGINE=InnoDB COMMENT='助教与教学分组多对多分配历史';

-- ============================================================================
-- 四、学生修读关系与“节次”落库
-- ============================================================================

-- 学生修读事实：连接学生、学年学期和教学分组。
-- Excel“节次”使用四段数值：周次-开始课节-结束课节-学生班内编号，
-- 例如 1-1-2-17。前三段用于定位“周1-1-2”班级，最后一段17实质是学生编号。
-- “节次”只是业务方沿用的列名，不将它解释为课次，也不人为创造班级编码。
-- normal_student_no 生成列配合唯一索引，保证每个学号普通修读最多一次。
-- 重修使用 RETAKE、attempt_no>1，并指向首次修读 original_enrollment_id。
CREATE TABLE student_course_enrollment (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    student_id BIGINT UNSIGNED NULL,
    term_id BIGINT UNSIGNED NOT NULL,
    teaching_group_id BIGINT UNSIGNED NULL,
    student_no_snapshot VARCHAR(50) NOT NULL,
    student_name_snapshot VARCHAR(100) NOT NULL,
    gender_snapshot ENUM('男','女','未知') NOT NULL DEFAULT '未知',
    admin_class_snapshot VARCHAR(100) NOT NULL,
    company_name_snapshot VARCHAR(100) NOT NULL,
    week_snapshot TINYINT UNSIGNED NOT NULL,
    start_period_snapshot TINYINT UNSIGNED NOT NULL,
    end_period_snapshot TINYINT UNSIGNED NOT NULL,
    section_no INT UNSIGNED NOT NULL COMMENT '导入列名为“节次”；实质是学生在班内的编号',
    section_value VARCHAR(80) GENERATED ALWAYS AS
        (CONCAT(week_snapshot, '-', start_period_snapshot, '-',
                end_period_snapshot, '-', section_no)) STORED
        COMMENT '完整导入值，如1-1-2-17',
    enrollment_type ENUM('NORMAL','RETAKE') NOT NULL DEFAULT 'NORMAL',
    attempt_no SMALLINT UNSIGNED NOT NULL DEFAULT 1,
    original_enrollment_id BIGINT UNSIGNED NULL,
    status ENUM('ACTIVE','COMPLETED','WITHDRAWN') NOT NULL DEFAULT 'ACTIVE',
    enrolled_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    completed_at DATETIME(3) NULL,
    normal_student_no VARCHAR(50) GENERATED ALWAYS AS
        (CASE WHEN enrollment_type='NORMAL' THEN student_no_snapshot ELSE NULL END) STORED,
    PRIMARY KEY (id),
    UNIQUE KEY uk_one_normal_course_per_student (normal_student_no),
    UNIQUE KEY uk_student_attempt (student_no_snapshot, attempt_no),
    UNIQUE KEY uk_group_section_no (teaching_group_id, section_no),
    KEY idx_enrollment_term_group (term_id, teaching_group_id, status),
    KEY idx_enrollment_student (student_id),
    CONSTRAINT ck_enrollment_week CHECK (week_snapshot BETWEEN 1 AND 5),
    CONSTRAINT ck_enrollment_period CHECK (
        start_period_snapshot IN (1,3,5,7)
        AND end_period_snapshot = start_period_snapshot + 1
    ),
    CONSTRAINT ck_enrollment_section_no CHECK (section_no > 0),
    CONSTRAINT ck_enrollment_attempt CHECK (
        (enrollment_type='NORMAL' AND attempt_no=1 AND original_enrollment_id IS NULL)
        OR (enrollment_type='RETAKE' AND attempt_no>1 AND original_enrollment_id IS NOT NULL)
    ),
    CONSTRAINT fk_enrollment_student FOREIGN KEY (student_id) REFERENCES student(id)
        ON DELETE SET NULL ON UPDATE RESTRICT,
    CONSTRAINT fk_enrollment_term FOREIGN KEY (term_id) REFERENCES academic_term(id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_enrollment_group FOREIGN KEY (teaching_group_id) REFERENCES teaching_group(id)
        ON DELETE SET NULL ON UPDATE RESTRICT,
    CONSTRAINT fk_enrollment_original FOREIGN KEY (original_enrollment_id) REFERENCES student_course_enrollment(id)
        ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB COMMENT='学生修读事实；普通修读仅一次，重修另建记录';

-- ============================================================================
-- 五、实际课次、考勤和劳动实践分数
-- ============================================================================

-- 课次：某个班级在具体日期发生的一次课。
-- 周次和连续课节来自班级安排，并在课次表中保存快照，防止以后调班影响历史。
-- score_deadline_at 在创建课次时根据配置计算并固化，避免修改配置影响旧课次。
CREATE TABLE course_session (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    term_id BIGINT UNSIGNED NOT NULL,
    teaching_group_id BIGINT UNSIGNED NULL,
    company_name_snapshot VARCHAR(100) NOT NULL,
    class_identifier_snapshot VARCHAR(30) NOT NULL COMMENT '如周1-1-2',
    week_snapshot TINYINT UNSIGNED NOT NULL,
    start_period_snapshot TINYINT UNSIGNED NOT NULL,
    end_period_snapshot TINYINT UNSIGNED NOT NULL,
    session_date DATE NOT NULL,
    score_deadline_at DATETIME(3) NOT NULL COMMENT '由配置计算并固化',
    max_score DECIMAL(5,2) NOT NULL DEFAULT 10.00,
    status ENUM('OPEN','LOCKED','ARCHIVED') NOT NULL DEFAULT 'OPEN',
    created_by_user_id BIGINT UNSIGNED NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_session_group_date (teaching_group_id, session_date),
    KEY idx_session_term_date (term_id, session_date),
    CONSTRAINT ck_session_week CHECK (week_snapshot BETWEEN 1 AND 5),
    CONSTRAINT ck_session_period CHECK (
        start_period_snapshot IN (1,3,5,7)
        AND end_period_snapshot = start_period_snapshot + 1
    ),
    CONSTRAINT ck_session_max_score CHECK (max_score > 0),
    CONSTRAINT fk_session_term FOREIGN KEY (term_id) REFERENCES academic_term(id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_session_group FOREIGN KEY (teaching_group_id) REFERENCES teaching_group(id)
        ON DELETE SET NULL ON UPDATE RESTRICT,
    CONSTRAINT fk_session_creator FOREIGN KEY (created_by_user_id) REFERENCES sys_user(id)
        ON DELETE SET NULL ON UPDATE RESTRICT
) ENGINE=InnoDB COMMENT='课次：某天发生的连续两节课';

-- 考勤：每个课次、每条学生修读记录只有一个当前考勤结果。
-- 考勤与分数拆开，避免请假/旷课状态和分数修改历史互相污染。
CREATE TABLE attendance_record (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    course_session_id BIGINT UNSIGNED NOT NULL,
    enrollment_id BIGINT UNSIGNED NOT NULL,
    attendance_type ENUM('NORMAL','LEAVE','ABSENCE') NOT NULL,
    recorded_by_user_id BIGINT UNSIGNED NULL,
    recorded_by_name_snapshot VARCHAR(100) NULL,
    version_no INT UNSIGNED NOT NULL DEFAULT 1,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_attendance_session_enrollment (course_session_id, enrollment_id),
    KEY idx_attendance_type (course_session_id, attendance_type),
    CONSTRAINT fk_attendance_session FOREIGN KEY (course_session_id) REFERENCES course_session(id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_attendance_enrollment FOREIGN KEY (enrollment_id) REFERENCES student_course_enrollment(id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_attendance_recorder FOREIGN KEY (recorded_by_user_id) REFERENCES sys_user(id)
        ON DELETE SET NULL ON UPDATE RESTRICT
) ENGINE=InnoDB COMMENT='考勤当前结果';

-- 当前分数：保存最新有效值。
-- score_value 与 score_mark 二选一：普通数值写 score_value，J/K 写 score_mark。
-- “首次非5分必须填写说明”等流程规则仍需要后端服务层校验。
CREATE TABLE score_record (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    course_session_id BIGINT UNSIGNED NOT NULL,
    enrollment_id BIGINT UNSIGNED NOT NULL,
    attendance_record_id BIGINT UNSIGNED NULL,
    score_value DECIMAL(5,2) NULL,
    score_mark ENUM('J','K') NULL,
    remark VARCHAR(500) NULL COMMENT '首次录入非5数值分时必填，由服务层校验',
    revision_count INT UNSIGNED NOT NULL DEFAULT 0,
    locked BOOLEAN NOT NULL DEFAULT FALSE,
    last_recorded_by_user_id BIGINT UNSIGNED NULL,
    last_recorded_by_name_snapshot VARCHAR(100) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_score_session_enrollment (course_session_id, enrollment_id),
    KEY idx_score_enrollment (enrollment_id),
    CONSTRAINT ck_score_value_or_mark CHECK (
        (score_value IS NOT NULL AND score_mark IS NULL AND score_value BETWEEN 0 AND 10)
        OR (score_value IS NULL AND score_mark IS NOT NULL)
    ),
    CONSTRAINT fk_score_session FOREIGN KEY (course_session_id) REFERENCES course_session(id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_score_enrollment FOREIGN KEY (enrollment_id) REFERENCES student_course_enrollment(id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_score_attendance FOREIGN KEY (attendance_record_id) REFERENCES attendance_record(id)
        ON DELETE SET NULL ON UPDATE RESTRICT,
    CONSTRAINT fk_score_recorder FOREIGN KEY (last_recorded_by_user_id) REFERENCES sys_user(id)
        ON DELETE SET NULL ON UPDATE RESTRICT
) ENGINE=InnoDB COMMENT='劳动实践课次分数当前值';

-- ============================================================================
-- 六、修改申请、分数历史与审批流水
-- ============================================================================

-- 修改申请：超期修改、第二次修改、K转J等场景先建立申请。
-- 审批通过后只在 edit_window_expires_at 之前允许修改，使用后写 used_at 再次锁定。
CREATE TABLE change_request (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    request_no VARCHAR(50) NOT NULL,
    request_type ENUM('SCORE_EDIT','ATTENDANCE_EDIT','TERM_UNARCHIVE') NOT NULL,
    target_table VARCHAR(80) NOT NULL,
    target_id BIGINT UNSIGNED NOT NULL,
    applicant_user_id BIGINT UNSIGNED NULL,
    applicant_name_snapshot VARCHAR(100) NOT NULL,
    reason VARCHAR(500) NOT NULL COMMENT 'trim后不能为空',
    status ENUM('PENDING','APPROVED','REJECTED','USED','CANCELLED','EXPIRED') NOT NULL DEFAULT 'PENDING',
    approved_at DATETIME(3) NULL,
    edit_window_expires_at DATETIME(3) NULL,
    used_at DATETIME(3) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_change_request_no (request_no),
    KEY idx_change_request_target (target_table, target_id),
    KEY idx_change_request_status (status, created_at),
    CONSTRAINT fk_change_request_applicant FOREIGN KEY (applicant_user_id) REFERENCES sys_user(id)
        ON DELETE SET NULL ON UPDATE RESTRICT,
    CONSTRAINT ck_change_request_reason CHECK (CHAR_LENGTH(TRIM(reason)) > 0)
) ENGINE=InnoDB COMMENT='超期或二次修改申请';

-- 分数修改历史：每次成功修改都追加一行，不能覆盖旧历史。
CREATE TABLE score_revision (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    score_record_id BIGINT UNSIGNED NOT NULL,
    revision_no INT UNSIGNED NOT NULL,
    old_score_value DECIMAL(5,2) NULL,
    old_score_mark ENUM('J','K') NULL,
    new_score_value DECIMAL(5,2) NULL,
    new_score_mark ENUM('J','K') NULL,
    change_reason VARCHAR(500) NOT NULL,
    change_request_id BIGINT UNSIGNED NULL,
    changed_by_user_id BIGINT UNSIGNED NULL,
    changed_by_name_snapshot VARCHAR(100) NOT NULL,
    changed_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_score_revision_no (score_record_id, revision_no),
    CONSTRAINT fk_score_revision_record FOREIGN KEY (score_record_id) REFERENCES score_record(id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_score_revision_request FOREIGN KEY (change_request_id) REFERENCES change_request(id)
        ON DELETE SET NULL ON UPDATE RESTRICT,
    CONSTRAINT fk_score_revision_user FOREIGN KEY (changed_by_user_id) REFERENCES sys_user(id)
        ON DELETE SET NULL ON UPDATE RESTRICT
) ENGINE=InnoDB COMMENT='分数不可变修改历史';

-- 审批动作流水：完整记录提交、通过、驳回、使用、取消、过期等动作。
CREATE TABLE approval_action (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    change_request_id BIGINT UNSIGNED NOT NULL,
    action ENUM('SUBMIT','APPROVE','REJECT','USE','CANCEL','EXPIRE') NOT NULL,
    operator_user_id BIGINT UNSIGNED NULL,
    operator_name_snapshot VARCHAR(100) NOT NULL,
    comment_text VARCHAR(500) NULL,
    action_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_approval_request_time (change_request_id, action_at),
    CONSTRAINT fk_approval_request FOREIGN KEY (change_request_id) REFERENCES change_request(id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_approval_operator FOREIGN KEY (operator_user_id) REFERENCES sys_user(id)
        ON DELETE SET NULL ON UPDATE RESTRICT
) ENGINE=InnoDB COMMENT='审批动作流水';

-- ============================================================================
-- 七、配置、通知、导入、导出与审计
-- ============================================================================

-- 业务配置：保存默认打分期限、审批修改窗口等可调整参数，避免写死在代码中。
CREATE TABLE business_config (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    config_key VARCHAR(100) NOT NULL,
    config_value VARCHAR(500) NOT NULL,
    value_type ENUM('STRING','INTEGER','DECIMAL','BOOLEAN','JSON') NOT NULL DEFAULT 'STRING',
    description VARCHAR(500) NULL,
    updated_by_user_id BIGINT UNSIGNED NULL,
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_business_config_key (config_key),
    CONSTRAINT fk_config_operator FOREIGN KEY (updated_by_user_id) REFERENCES sys_user(id)
        ON DELETE SET NULL ON UPDATE RESTRICT
) ENGINE=InnoDB COMMENT='业务配置；期限等不得硬编码';

-- 通知主体：一条业务通知只保存一次正文。
CREATE TABLE notification (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    notification_type ENUM('UNSCORED_REMINDER','DEADLINE_WARNING','APPROVAL_RESULT','SYSTEM') NOT NULL,
    title VARCHAR(200) NOT NULL,
    content TEXT NOT NULL,
    related_table VARCHAR(80) NULL,
    related_id BIGINT UNSIGNED NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_notification_related (related_table, related_id)
) ENGINE=InnoDB COMMENT='通知内容';

-- 通知接收者：一条通知可发给多个用户，并分别记录送达和阅读时间。
CREATE TABLE notification_recipient (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    notification_id BIGINT UNSIGNED NOT NULL,
    user_id BIGINT UNSIGNED NULL,
    username_snapshot VARCHAR(80) NOT NULL,
    read_at DATETIME(3) NULL,
    delivered_at DATETIME(3) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_notification_recipient (notification_id, username_snapshot),
    KEY idx_recipient_user_read (user_id, read_at),
    CONSTRAINT fk_recipient_notification FOREIGN KEY (notification_id) REFERENCES notification(id)
        ON DELETE CASCADE ON UPDATE RESTRICT,
    CONSTRAINT fk_recipient_user FOREIGN KEY (user_id) REFERENCES sys_user(id)
        ON DELETE SET NULL ON UPDATE RESTRICT
) ENGINE=InnoDB COMMENT='通知接收与已读状态';

-- 导入模板版本：学生和助教模板分别建定义，但两者可以配置同样六列。
CREATE TABLE import_template_definition (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    template_code VARCHAR(50) NOT NULL,
    template_name VARCHAR(100) NOT NULL,
    template_version INT UNSIGNED NOT NULL DEFAULT 1,
    status ENUM('ACTIVE','INACTIVE') NOT NULL DEFAULT 'ACTIVE',
    active_template_code VARCHAR(50) GENERATED ALWAYS AS
        (CASE WHEN status='ACTIVE' THEN template_code ELSE NULL END) STORED,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_import_template_version (template_code, template_version),
    UNIQUE KEY uk_one_active_template (active_template_code)
) ENGINE=InnoDB COMMENT='导入模板版本';

-- 导入模板列：记录列号、中文表头、程序字段名、是否必填和校验提示。
-- 01_seed_reference.sql 会为学生、助教各写入六列定义。
CREATE TABLE import_template_column (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    template_id BIGINT UNSIGNED NOT NULL,
    column_no TINYINT UNSIGNED NOT NULL COMMENT '从1开始',
    column_name VARCHAR(50) NOT NULL,
    field_code VARCHAR(50) NOT NULL,
    required_flag BOOLEAN NOT NULL DEFAULT TRUE,
    value_type ENUM('TEXT','POSITIVE_INTEGER','GENDER','SECTION') NOT NULL DEFAULT 'TEXT',
    max_length INT UNSIGNED NULL,
    validation_hint VARCHAR(500) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_template_column_no (template_id, column_no),
    UNIQUE KEY uk_template_column_name (template_id, column_name),
    CONSTRAINT fk_template_column_definition FOREIGN KEY (template_id)
        REFERENCES import_template_definition(id) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE=InnoDB COMMENT='学生和助教统一六列导入定义';

-- 导入批次：一次上传对应一条批次记录。
-- term_id 不来自 Excel，而是在上传页面选择学年学期后随接口传入。
-- 只要存在一条校验错误，批次应进入 REJECTED，业务表不得部分写入。
CREATE TABLE data_import_batch (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    batch_no VARCHAR(50) NOT NULL,
    import_type ENUM('STUDENT','ASSISTANT') NOT NULL,
    template_id BIGINT UNSIGNED NOT NULL,
    term_id BIGINT UNSIGNED NOT NULL COMMENT '页面/API额外指定',
    original_file_name VARCHAR(255) NOT NULL,
    file_sha256 CHAR(64) NULL,
    total_rows INT UNSIGNED NOT NULL DEFAULT 0,
    valid_rows INT UNSIGNED NOT NULL DEFAULT 0,
    invalid_rows INT UNSIGNED NOT NULL DEFAULT 0,
    status ENUM('UPLOADED','VALIDATING','REJECTED','IMPORTING','SUCCEEDED','FAILED','ROLLED_BACK') NOT NULL DEFAULT 'UPLOADED',
    started_by_user_id BIGINT UNSIGNED NULL,
    started_by_name_snapshot VARCHAR(100) NOT NULL,
    started_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    finished_at DATETIME(3) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_import_batch_no (batch_no),
    KEY idx_import_batch_status_time (status, started_at),
    CONSTRAINT ck_import_batch_counts CHECK (valid_rows + invalid_rows <= total_rows),
    CONSTRAINT fk_import_batch_template FOREIGN KEY (template_id) REFERENCES import_template_definition(id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_import_batch_term FOREIGN KEY (term_id) REFERENCES academic_term(id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_import_batch_user FOREIGN KEY (started_by_user_id) REFERENCES sys_user(id)
        ON DELETE SET NULL ON UPDATE RESTRICT
) ENGINE=InnoDB COMMENT='导入批次；任一行失败时整批不落业务表';

-- 导入错误：一条错误精确记录 Excel 行、列、字段名、原始值和中文提示。
-- 表头缺失时 row_no 可为空或写1；普通单元格错误必须写真实 Excel 行号。
CREATE TABLE data_import_error (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    batch_id BIGINT UNSIGNED NOT NULL,
    row_no INT UNSIGNED NULL COMMENT 'Excel行号；缺表头时可为空或1',
    column_no TINYINT UNSIGNED NULL,
    column_name VARCHAR(50) NULL,
    error_code VARCHAR(50) NOT NULL,
    error_message VARCHAR(500) NOT NULL,
    raw_value VARCHAR(1000) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_import_error_batch_row (batch_id, row_no, column_no),
    CONSTRAINT fk_import_error_batch FOREIGN KEY (batch_id) REFERENCES data_import_batch(id)
        ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE=InnoDB COMMENT='精确到行列的导入错误';

-- 导出任务：大数据导出异步执行，完成后在导出中心下载，避免业务请求长时间阻塞。
CREATE TABLE export_job (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    job_no VARCHAR(50) NOT NULL,
    export_type VARCHAR(50) NOT NULL,
    filter_json JSON NOT NULL,
    status ENUM('PENDING','RUNNING','SUCCEEDED','FAILED','EXPIRED') NOT NULL DEFAULT 'PENDING',
    file_path VARCHAR(500) NULL,
    failure_reason VARCHAR(1000) NULL,
    requested_by_user_id BIGINT UNSIGNED NULL,
    requested_by_name_snapshot VARCHAR(100) NOT NULL,
    requested_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    finished_at DATETIME(3) NULL,
    expires_at DATETIME(3) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_export_job_no (job_no),
    KEY idx_export_job_user_status (requested_by_user_id, status),
    CONSTRAINT fk_export_job_user FOREIGN KEY (requested_by_user_id) REFERENCES sys_user(id)
        ON DELETE SET NULL ON UPDATE RESTRICT
) ENGINE=InnoDB COMMENT='导出中心异步任务';

-- 操作日志：保存操作者快照、目标、修改前后 JSON、结果和失败原因。
CREATE TABLE operation_log (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    actor_user_id BIGINT UNSIGNED NULL,
    actor_username_snapshot VARCHAR(80) NOT NULL,
    actor_name_snapshot VARCHAR(100) NOT NULL,
    operation_type VARCHAR(80) NOT NULL,
    target_table VARCHAR(80) NULL,
    target_id BIGINT UNSIGNED NULL,
    request_id VARCHAR(80) NULL,
    description VARCHAR(1000) NOT NULL,
    before_json JSON NULL,
    after_json JSON NULL,
    ip_address VARCHAR(64) NULL,
    result ENUM('SUCCESS','FAILURE') NOT NULL,
    failure_reason VARCHAR(1000) NULL,
    operated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_operation_actor_time (actor_user_id, operated_at),
    KEY idx_operation_target (target_table, target_id, operated_at),
    KEY idx_operation_type_time (operation_type, operated_at),
    CONSTRAINT fk_operation_actor FOREIGN KEY (actor_user_id) REFERENCES sys_user(id)
        ON DELETE SET NULL ON UPDATE RESTRICT
) ENGINE=InnoDB COMMENT='操作审计日志';

-- ============================================================================
-- 八、报表汇总过渡表
-- ============================================================================

-- 学生课程汇总：由后台任务从明细表计算，查询报表时不直接扫描大量考勤记录。
CREATE TABLE student_course_summary (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    enrollment_id BIGINT UNSIGNED NOT NULL,
    normal_count INT UNSIGNED NOT NULL DEFAULT 0,
    leave_count INT UNSIGNED NOT NULL DEFAULT 0,
    absence_count INT UNSIGNED NOT NULL DEFAULT 0,
    scored_session_count INT UNSIGNED NOT NULL DEFAULT 0,
    labor_practice_score DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    last_calculated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_student_summary_enrollment (enrollment_id),
    CONSTRAINT fk_student_summary_enrollment FOREIGN KEY (enrollment_id)
        REFERENCES student_course_enrollment(id) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB COMMENT='学生课程汇总过渡表';

-- 教学分组汇总：保存分组人数、助教数、课次数和未打分数等高频统计结果。
CREATE TABLE teaching_group_summary (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    teaching_group_id BIGINT UNSIGNED NULL,
    term_id BIGINT UNSIGNED NOT NULL,
    company_name_snapshot VARCHAR(100) NOT NULL,
    class_identifier_snapshot VARCHAR(30) NOT NULL COMMENT '如周1-1-2',
    enrolled_count INT UNSIGNED NOT NULL DEFAULT 0,
    assistant_count INT UNSIGNED NOT NULL DEFAULT 0,
    session_count INT UNSIGNED NOT NULL DEFAULT 0,
    unscored_count INT UNSIGNED NOT NULL DEFAULT 0,
    last_calculated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_group_summary
        (term_id, company_name_snapshot, class_identifier_snapshot),
    CONSTRAINT fk_group_summary_group FOREIGN KEY (teaching_group_id) REFERENCES teaching_group(id)
        ON DELETE SET NULL ON UPDATE RESTRICT,
    CONSTRAINT fk_group_summary_term FOREIGN KEY (term_id) REFERENCES academic_term(id)
        ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB COMMENT='教学分组汇总过渡表';

-- 恢复外键检查。若前面的建表语句失败，应先修正错误，不要直接忽略。
SET FOREIGN_KEY_CHECKS = 1;
SELECT 'labor_management_v2 schema created' AS result;
