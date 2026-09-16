-- =====================================================================
-- 劳动学时管理系统 - 完整数据库初始化脚本
-- 适用场景：全新安装数据库（会删除并重建同名业务表）
-- 规范：InnoDB + utf8mb4 + utf8mb4_unicode_ci；关联字段只建索引，不建外键
-- =====================================================================

CREATE DATABASE IF NOT EXISTS labor_management
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE labor_management;

-- 1. 系统用户表：教师、助教和管理员的登录账号
DROP TABLE IF EXISTS sys_user;
CREATE TABLE sys_user (
    id                         BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    username                   VARCHAR(50)  NOT NULL COMMENT '登录用户名',
    password_hash              VARCHAR(100) NOT NULL COMMENT '密码哈希(BCrypt)',
    real_name                  VARCHAR(50)  NOT NULL COMMENT '真实姓名',
    status                     TINYINT      NOT NULL DEFAULT 1 COMMENT '状态: 1=启用, 0=禁用',
    first_login                TINYINT      NOT NULL DEFAULT 1 COMMENT '是否首次登录: 1=是, 0=否',
    last_password_change_time  DATETIME              DEFAULT NULL COMMENT '最后密码修改时间',
    student_id                 BIGINT                DEFAULT NULL COMMENT '关联学生ID，普通教师和管理员为空',
    created_at                 DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at                 DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
                                           ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted                    TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0=未删除, 1=已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_sys_user_username (username),
    KEY idx_sys_user_student_id (student_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统用户表';

-- 2. 系统角色表
DROP TABLE IF EXISTS sys_role;
CREATE TABLE sys_role (
    id          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    role_code   VARCHAR(50) NOT NULL COMMENT '角色编码: SUPER_ADMIN/TEACHER/ASSISTANT',
    role_name   VARCHAR(50) NOT NULL COMMENT '角色名称',
    created_at  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted     TINYINT     NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0=未删除, 1=已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_sys_role_role_code (role_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统角色表';

-- 3. 用户-角色关联表
DROP TABLE IF EXISTS user_role;
CREATE TABLE user_role (
    id          BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    user_id     BIGINT   NOT NULL COMMENT '用户ID，对应sys_user.id',
    role_id     BIGINT   NOT NULL COMMENT '角色ID，对应sys_role.id',
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted     TINYINT  NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0=未删除, 1=已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_role (user_id, role_id),
    KEY idx_user_role_user_id (user_id),
    KEY idx_user_role_role_id (role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户角色关联表';

-- 4. 公司表
DROP TABLE IF EXISTS company;
CREATE TABLE company (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    name        VARCHAR(100) NOT NULL COMMENT '公司名称',
    sort_order  INT          NOT NULL DEFAULT 0 COMMENT '排序号',
    status      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态: 1=启用, 0=停用',
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0=未删除, 1=已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_company_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='公司表';

-- 5. 班级表：某公司在指定周次和节次的一次劳动安排
DROP TABLE IF EXISTS classes;
CREATE TABLE classes (
    id             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    class_name     VARCHAR(100) NOT NULL COMMENT '班级名称，如茶园-第1周-1~2节',
    class_code     VARCHAR(50)           DEFAULT NULL COMMENT '班级编码，格式W-S-E，如1-1-2',
    company_id     BIGINT       NOT NULL COMMENT '所属公司ID，对应company.id',
    week           INT          NOT NULL COMMENT '周次',
    start_session  INT          NOT NULL COMMENT '开始节次',
    end_session    INT          NOT NULL COMMENT '结束节次',
    academic_year  VARCHAR(20)  NOT NULL COMMENT '学年，如2024-2025',
    status         TINYINT      NOT NULL DEFAULT 1 COMMENT '状态: 1=启用, 0=归档',
    created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted        TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0=未删除, 1=已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_classes_company_session (company_id, week, start_session, end_session),
    KEY idx_classes_company (company_id),
    KEY idx_classes_class_code (class_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='班级表';

-- 6. 学生表：助教负责的班级通过 assistant_class 维护
DROP TABLE IF EXISTS student;
CREATE TABLE student (
    id                   BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    student_id           VARCHAR(50)  NOT NULL COMMENT '学号',
    name                 VARCHAR(50)  NOT NULL COMMENT '学生姓名',
    class_id             BIGINT                DEFAULT NULL COMMENT '所属班级ID，助教可暂时为空',
    gender               TINYINT      NOT NULL DEFAULT 0 COMMENT '性别: 0=未知, 1=男, 2=女',
    original_major       VARCHAR(100)          DEFAULT NULL COMMENT '原始专业',
    student_no_in_class  INT                   DEFAULT NULL COMMENT '班级内编号，完整编号W-S-E-N中的N',
    status               TINYINT      NOT NULL DEFAULT 1 COMMENT '状态: 1=在读, 0=停用',
    is_assistant         TINYINT      NOT NULL DEFAULT 0 COMMENT '是否助教: 0=否, 1=是',
    created_at           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted              TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0=未删除, 1=已删除',
    active_student_no_in_class INT GENERATED ALWAYS AS (
        CASE WHEN deleted = 0 THEN student_no_in_class ELSE NULL END
    ) STORED COMMENT '仅用于约束未删除学生的班内编号唯一',
    PRIMARY KEY (id),
    UNIQUE KEY uk_student_student_id (student_id),
    UNIQUE KEY uk_student_active_class_no (class_id, active_student_no_in_class),
    KEY idx_student_class_id (class_id),
    KEY idx_student_is_assistant (is_assistant)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学生表';

-- 7. 教师-班级关联表；user_id 与C的实体及Mapper保持一致
DROP TABLE IF EXISTS teacher_class;
CREATE TABLE teacher_class (
    id          BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    user_id     BIGINT   NOT NULL COMMENT '教师账号ID，对应sys_user.id',
    class_id    BIGINT   NOT NULL COMMENT '负责班级ID，对应classes.id',
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted     TINYINT  NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0=未删除, 1=已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_teacher_class (user_id, class_id),
    KEY idx_teacher_class_user (user_id),
    KEY idx_teacher_class_class (class_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='教师负责班级关联表（多对多）';

-- 8. 助教-班级关联表（多对多）
DROP TABLE IF EXISTS assistant_class;
CREATE TABLE assistant_class (
    id                    BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    assistant_student_id  BIGINT   NOT NULL COMMENT '助教学生记录ID，对应student.id',
    class_id              BIGINT   NOT NULL COMMENT '负责班级ID，对应classes.id',
    created_at            DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at            DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted               TINYINT  NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0=未删除, 1=已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_assistant_class (assistant_student_id, class_id),
    KEY idx_assistant_class_student (assistant_student_id),
    KEY idx_assistant_class_class (class_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='助教负责班级关联表（多对多）';

-- 9. 劳动课周次表
DROP TABLE IF EXISTS attendance_session;
CREATE TABLE attendance_session (
    id               BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    class_id         BIGINT   NOT NULL COMMENT '班级ID，对应classes.id',
    week_no          INT      NOT NULL COMMENT '周次',
    session_date     DATE              DEFAULT NULL COMMENT '实际上课日期',
    is_last_session  TINYINT  NOT NULL DEFAULT 0 COMMENT '是否最后一次课: 0=否, 1=是',
    status           TINYINT  NOT NULL DEFAULT 1 COMMENT '状态: 1=可编辑, 0=已封存',
    created_by       BIGINT            DEFAULT NULL COMMENT '创建人账号ID',
    updated_by       BIGINT            DEFAULT NULL COMMENT '最后修改人账号ID',
    created_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted          TINYINT  NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0=未删除, 1=已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_attendance_session_class_week (class_id, week_no),
    KEY idx_attendance_session_class (class_id),
    KEY idx_attendance_session_date (session_date),
    KEY idx_attendance_session_creator (created_by)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='劳动课周次表';

-- 10. 劳动课考勤与打分记录表
DROP TABLE IF EXISTS attendance_record;
CREATE TABLE attendance_record (
    id               BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    session_id       BIGINT        NOT NULL COMMENT '劳动课周次ID，对应attendance_session.id',
    student_id       BIGINT        NOT NULL COMMENT '学生记录ID，对应student.id',
    attendance_type  VARCHAR(10)   NOT NULL DEFAULT 'NORMAL' COMMENT '考勤类型: NORMAL/J/K',
    score            DECIMAL(5,2) NOT NULL DEFAULT 5.00 COMMENT '本次劳动课分数',
    remark           VARCHAR(255)           DEFAULT NULL COMMENT '备注',
    recorded_by      BIGINT                 DEFAULT NULL COMMENT '登记人账号ID',
    created_at       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted          TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0=未删除, 1=已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_attendance_record_session_student (session_id, student_id),
    KEY idx_attendance_record_session (session_id),
    KEY idx_attendance_record_student (student_id),
    KEY idx_attendance_record_operator (recorded_by),
    CONSTRAINT chk_attendance_record_score CHECK (score >= 0 AND score <= 10)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='劳动课考勤与打分记录表';

-- 11. 换证考试最终成绩表
DROP TABLE IF EXISTS certificate_score;
CREATE TABLE certificate_score (
    id             BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    student_id     BIGINT        NOT NULL COMMENT '学生记录ID，对应student.id',
    class_id       BIGINT        NOT NULL COMMENT '录入成绩时所属班级ID',
    academic_year  VARCHAR(20)   NOT NULL COMMENT '学年，如2024-2025',
    semester       TINYINT       NOT NULL DEFAULT 1 COMMENT '学期: 1=第一学期, 2=第二学期',
    final_score    DECIMAL(5,2) NOT NULL COMMENT '换证考试最终成绩',
    remark         VARCHAR(255)           DEFAULT NULL COMMENT '备注',
    recorded_by    BIGINT                 DEFAULT NULL COMMENT '录入人账号ID',
    created_at     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted        TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0=未删除, 1=已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_certificate_score_student_term (student_id, academic_year, semester),
    KEY idx_certificate_score_class (class_id),
    KEY idx_certificate_score_operator (recorded_by),
    CONSTRAINT chk_certificate_score_value CHECK (final_score >= 0 AND final_score <= 100),
    CONSTRAINT chk_certificate_score_semester CHECK (semester IN (1, 2))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='换证考试最终成绩表';

-- 12. 操作记录表
DROP TABLE IF EXISTS operation_log;
CREATE TABLE operation_log (
    id                BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    operator_user_id  BIGINT                DEFAULT NULL COMMENT '操作人账号ID',
    module_name       VARCHAR(50)  NOT NULL COMMENT '模块名称',
    operation_type    VARCHAR(30)  NOT NULL COMMENT '操作类型: CREATE/UPDATE/DELETE/IMPORT/EXPORT',
    target_type       VARCHAR(50)           DEFAULT NULL COMMENT '操作对象类型',
    target_id         BIGINT                DEFAULT NULL COMMENT '操作对象ID',
    description       VARCHAR(500)          DEFAULT NULL COMMENT '操作说明',
    before_data       JSON                  DEFAULT NULL COMMENT '修改前数据',
    after_data        JSON                  DEFAULT NULL COMMENT '修改后数据',
    client_ip         VARCHAR(64)           DEFAULT NULL COMMENT '客户端IP',
    created_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    PRIMARY KEY (id),
    KEY idx_operation_log_operator (operator_user_id),
    KEY idx_operation_log_module (module_name),
    KEY idx_operation_log_target (target_type, target_id),
    KEY idx_operation_log_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统操作记录表';

-- 初始化基础角色和公司数据
INSERT INTO sys_role (role_code, role_name) VALUES
    ('SUPER_ADMIN', '超级管理员'),
    ('TEACHER', '教师'),
    ('ASSISTANT', '助教');

INSERT INTO company (name, sort_order) VALUES
    ('茶园', 1),
    ('果园', 2),
    ('待定1', 3),
    ('待定2', 4),
    ('待定3', 5),
    ('待定4', 6),
    ('待定5', 7),
    ('待定6', 8);
