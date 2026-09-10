-- =====================================================================
-- 劳动学时管理系统 - 测试数据：教师 + 助教账号
-- 阶段5：用户管理测试用
--
-- 默认密码：cdjcc123456
-- 密码哈希（BCrypt）：
--   $2a$10$lEXUo.SkavGolbwCYXzAlOQFQgnNPABcRz0APsoUBPAsdsCEqvcWa
--   该哈希对应明文 cdjcc123456，由 BCryptPasswordEncoder 生成
--
-- 角色记录在 init_schema.sql 中插入：
--   SUPER_ADMIN id 一般为 1
--   TEACHER id 一般为 2
--   ASSISTANT id 一般为 3
-- =====================================================================

USE labor_management;

-- ---------------------------------------------------------------------
-- 教师测试账号
-- ---------------------------------------------------------------------
INSERT INTO sys_user (username, password_hash, real_name, status, first_login)
SELECT 'teacher1',
       '$2a$10$lEXUo.SkavGolbwCYXzAlOQFQgnNPABcRz0APsoUBPAsdsCEqvcWa',
       '张老师',
       1,
       1
WHERE NOT EXISTS (
    SELECT 1 FROM sys_user WHERE username = 'teacher1'
);

-- 关联 teacher1 与 TEACHER 角色
INSERT INTO user_role (user_id, role_id)
SELECT u.id, r.id
FROM sys_user u, sys_role r
WHERE u.username = 'teacher1'
  AND r.role_code = 'TEACHER'
  AND NOT EXISTS (
      SELECT 1 FROM user_role ur
      WHERE ur.user_id = u.id AND ur.role_id = r.id
  );

-- ---------------------------------------------------------------------
-- 助教测试账号
-- ---------------------------------------------------------------------
INSERT INTO sys_user (username, password_hash, real_name, status, first_login)
SELECT 'assistant1',
       '$2a$10$lEXUo.SkavGolbwCYXzAlOQFQgnNPABcRz0APsoUBPAsdsCEqvcWa',
       '李助教',
       1,
       1
WHERE NOT EXISTS (
    SELECT 1 FROM sys_user WHERE username = 'assistant1'
);

-- 关联 assistant1 与 ASSISTANT 角色
INSERT INTO user_role (user_id, role_id)
SELECT u.id, r.id
FROM sys_user u, sys_role r
WHERE u.username = 'assistant1'
  AND r.role_code = 'ASSISTANT'
  AND NOT EXISTS (
      SELECT 1 FROM user_role ur
      WHERE ur.user_id = u.id AND ur.role_id = r.id
  );
