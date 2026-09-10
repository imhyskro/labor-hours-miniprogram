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
    last_password_change_time   DATETIME              DEFAULT NULL    COMMENT '最后密码修改时间',
    created_at                  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP                                 COMMENT '创建时间',
    updated_at                  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP   COMMENT '更新时间',
    deleted                     TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除: 0=未删除, 1=已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_sys_user_username (username)
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
    id          BIGINT      NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    student_id  VARCHAR(50) NOT NULL                COMMENT '学号',
    name        VARCHAR(50) NOT NULL                COMMENT '学生姓名',
    class_id    BIGINT       NOT NULL                COMMENT '班级ID',
    gender      TINYINT      NOT NULL DEFAULT 0      COMMENT '性别: 0=未知, 1=男, 2=女',
    status      TINYINT      NOT NULL DEFAULT 1      COMMENT '状态: 1=在读, 0=停用',
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP                                 COMMENT '创建时间',
    updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP   COMMENT '更新时间',
    deleted     TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除: 0=未删除, 1=已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_student_student_id (student_id),
    KEY idx_student_class_id (class_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学生表';

-- ---------------------------------------------------------------------
-- 初始化角色数据
-- ---------------------------------------------------------------------
INSERT INTO sys_role (role_code, role_name) VALUES
    ('SUPER_ADMIN', '超级管理员'),
    ('TEACHER', '教师'),
    ('ASSISTANT', '助教');
