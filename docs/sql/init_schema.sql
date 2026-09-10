-- =====================================================================
-- 劳动学时管理系统 - 数据库初始化脚本（阶段1：基础表）
-- 阶段1 仅创建基础表：sys_user / sys_role / user_role / classes / student
-- 规范：InnoDB + utf8mb4 + utf8mb4_unicode_ci
--       主键 BIGINT AUTO_INCREMENT
--       外键建立索引（不使用 FK 约束，避免级联删除破坏历史数据）
--       逻辑删除字段 deleted（0=未删除，1=已删除）
-- =====================================================================

CREATE DATABASE IF NOT EXISTS labor_management
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE labor_management;

-- ---------------------------------------------------------------------
-- 1. 系统用户表 sys_user
--    教师/助教/管理员的登录账号
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS sys_user;
CREATE TABLE sys_user (
    id                          BIGINT       NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    username                    VARCHAR(50)  NOT NULL                COMMENT '登录用户名',
    password_hash               VARCHAR(100) NOT NULL                COMMENT '密码哈希(BCrypt)',
    real_name                   VARCHAR(50)  NOT NULL                COMMENT '真实姓名',
    status                      TINYINT      NOT NULL DEFAULT 1      COMMENT '状态: 1=启用, 0=禁用',
    first_login                 TINYINT      NOT NULL DEFAULT 1      COMMENT '是否首次登录: 1=是, 0=否',
    last_password_change_time   DATETIME DEFAULT NULL COMMENT '最后密码修改时间',
	student_id                  BIGINT DEFAULT NULL COMMENT '助教账号关联的学生记录主键',
	created_at                  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
	updated_at                  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP   COMMENT '更新时间',
    deleted                     TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除: 0=未删除, 1=已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_sys_user_username (username),
    KEY idx_sys_user_student_id (student_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统用户表';

-- ---------------------------------------------------------------------
-- 2. 系统角色表 sys_role
--    角色编码: SUPER_ADMIN / TEACHER / ASSISTANT
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS sys_role;
CREATE TABLE sys_role (
    id          BIGINT      NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    role_code   VARCHAR(50) NOT NULL                COMMENT '角色编码: SUPER_ADMIN/TEACHER/ASSISTANT',
    role_name   VARCHAR(50) NOT NULL                COMMENT '角色名称',
    created_at  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP                                 COMMENT '创建时间',
    updated_at  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP   COMMENT '更新时间',
    deleted     TINYINT     NOT NULL DEFAULT 0      COMMENT '逻辑删除: 0=未删除, 1=已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_sys_role_role_code (role_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统角色表';

-- ---------------------------------------------------------------------
-- 3. 用户-角色关联表 user_role
--    用户与角色的多对多关系
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS user_role;
CREATE TABLE user_role (
    id          BIGINT  NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    user_id     BIGINT  NOT NULL                COMMENT '用户ID',
    role_id     BIGINT  NOT NULL                COMMENT '角色ID',
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP                                COMMENT '创建时间',
    updated_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP    COMMENT '更新时间',
    deleted     TINYINT NOT NULL DEFAULT 0      COMMENT '逻辑删除: 0=未删除, 1=已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_role (user_id, role_id),
    KEY idx_user_role_user_id (user_id),
    KEY idx_user_role_role_id (role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户角色关联表';

-- ---------------------------------------------------------------------
-- 4. 班级表 classes
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS classes;
CREATE TABLE classes (
    id            BIGINT       NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    class_name    VARCHAR(100) NOT NULL                COMMENT '班级名称',
    class_code    VARCHAR(50)  NOT NULL                COMMENT '班级编号',
    academic_year VARCHAR(20) NOT NULL                COMMENT '学年, 如 2024-2025',
    status        TINYINT      NOT NULL DEFAULT 1      COMMENT '状态: 1=启用, 0=归档',
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP                                 COMMENT '创建时间',
    updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP   COMMENT '更新时间',
    deleted       TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除: 0=未删除, 1=已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_classes_class_code (class_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='班级表';

-- ---------------------------------------------------------------------
-- 5. 学生表 student
--    学生为业务数据对象，非登录用户
--    助教通过 sys_user.student_id 关联学生
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS student;
CREATE TABLE student (
    id                  BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    student_id          VARCHAR(50) NOT NULL COMMENT '学号',
    name                VARCHAR(50) NOT NULL COMMENT '学生姓名',
    class_id            BIGINT               DEFAULT NULL COMMENT '所属班级ID，助教导入时可暂时为空',
    gender              TINYINT     NOT NULL DEFAULT 0 COMMENT '性别: 0=未知, 1=男, 2=女',
    status              TINYINT     NOT NULL DEFAULT 1 COMMENT '状态: 1=在读, 0=停用',
    is_assistant        TINYINT     NOT NULL DEFAULT 0 COMMENT '是否助教: 0=否, 1=是',
    assigned_class_id   BIGINT               DEFAULT NULL COMMENT '助教负责的班级ID',
    created_at          DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at          DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP
                                          ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted             TINYINT     NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0=未删除, 1=已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_student_student_id (student_id),
    KEY idx_student_class_id (class_id),
    KEY idx_student_assigned_class_id (assigned_class_id)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='学生表';
  
  -- ---------------------------------------------------------------------
-- 6. 教师-班级关系表
-- 一个教师可负责多个班级，一个班级也可由多个教师负责
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS teacher_class;
CREATE TABLE teacher_class (
    id                BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    teacher_user_id   BIGINT   NOT NULL COMMENT '教师账号ID，对应sys_user.id',
    class_id          BIGINT   NOT NULL COMMENT '班级ID，对应classes.id',
    created_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
                                 ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted           TINYINT  NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0=未删除, 1=已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_teacher_class (teacher_user_id, class_id),
    KEY idx_teacher_class_teacher (teacher_user_id),
    KEY idx_teacher_class_class (class_id)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='教师与班级关系表';


-- ---------------------------------------------------------------------
-- 7. 劳动课周次表
-- 教师为每个班级设置实际劳动课周次
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS attendance_session;
CREATE TABLE attendance_session (
    id                BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    class_id          BIGINT      NOT NULL COMMENT '班级ID，对应classes.id',
    week_no           INT         NOT NULL COMMENT '周次',
    session_date      DATE                 DEFAULT NULL COMMENT '实际上课日期',
    is_last_session   TINYINT     NOT NULL DEFAULT 0 COMMENT '是否最后一次课: 0=否, 1=是',
    status            TINYINT     NOT NULL DEFAULT 1 COMMENT '状态: 1=可编辑, 0=已封存',
    created_by        BIGINT               DEFAULT NULL COMMENT '创建人账号ID',
    updated_by        BIGINT               DEFAULT NULL COMMENT '最后修改人账号ID',
    created_at        DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at        DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP
                                      ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted           TINYINT     NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0=未删除, 1=已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_attendance_session_class_week (class_id, week_no),
    KEY idx_attendance_session_class (class_id),
    KEY idx_attendance_session_date (session_date)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='劳动课周次表';


-- ---------------------------------------------------------------------
-- 8. 劳动课考勤与打分记录表
-- 每名学生每个周次只能有一条有效记录
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS attendance_record;
CREATE TABLE attendance_record (
    id                BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    session_id        BIGINT        NOT NULL COMMENT '劳动课周次ID',
    student_id        BIGINT        NOT NULL COMMENT '学生记录ID，对应student.id',
    attendance_type   VARCHAR(10)   NOT NULL DEFAULT 'NORMAL'
                                               COMMENT '考勤类型: NORMAL/J/K',
    score             DECIMAL(5,2) NOT NULL DEFAULT 5.00 COMMENT '本次劳动课分数',
    remark            VARCHAR(255)           DEFAULT NULL COMMENT '备注',
    recorded_by       BIGINT                 DEFAULT NULL COMMENT '登记人账号ID',
    created_at        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP
                                        ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted           TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0=未删除, 1=已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_attendance_record_session_student (session_id, student_id),
    KEY idx_attendance_record_session (session_id),
    KEY idx_attendance_record_student (student_id),
    KEY idx_attendance_record_operator (recorded_by),
    CONSTRAINT chk_attendance_record_score
        CHECK (score >= 0 AND score <= 10)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='劳动课考勤与打分记录表';


-- ---------------------------------------------------------------------
-- 9. 换证考试最终成绩表
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS certificate_score;
CREATE TABLE certificate_score (
    id                BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    student_id        BIGINT        NOT NULL COMMENT '学生记录ID，对应student.id',
    class_id          BIGINT        NOT NULL COMMENT '录入成绩时所属班级ID',
    academic_year     VARCHAR(20)   NOT NULL COMMENT '学年，如2024-2025',
    semester          TINYINT       NOT NULL DEFAULT 1 COMMENT '学期: 1=第一学期, 2=第二学期',
    final_score       DECIMAL(5,2) NOT NULL COMMENT '换证考试最终成绩',
    remark            VARCHAR(255)           DEFAULT NULL COMMENT '备注',
    recorded_by       BIGINT                 DEFAULT NULL COMMENT '录入人账号ID',
    created_at        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP
                                        ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted           TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0=未删除, 1=已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_certificate_score_student_term
        (student_id, academic_year, semester),
    KEY idx_certificate_score_class (class_id),
    KEY idx_certificate_score_operator (recorded_by)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='换证考试最终成绩表';


-- ---------------------------------------------------------------------
-- 10. 操作记录表
-- 记录考勤、成绩、导入、删除等关键操作
-- ---------------------------------------------------------------------
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
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='系统操作记录表';
  
-- ---------------------------------------------------------------------
-- 初始化角色数据
-- ---------------------------------------------------------------------
INSERT INTO sys_role (role_code, role_name) VALUES
    ('SUPER_ADMIN', '超级管理员'),
    ('TEACHER', '教师'),
    ('ASSISTANT', '助教');
