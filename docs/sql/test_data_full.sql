-- =====================================================================
-- 劳动学时管理系统 - 小程序完整测试数据
--
-- 使用前提：先执行 init_schema.sql（该脚本会创建 labor_management_test）。
-- 本脚本可重复执行：使用固定测试账号与测试学号，不删除非测试数据。
-- 默认密码：cdjcc123456
-- =====================================================================

USE labor_management_test;

START TRANSACTION;

-- ---------------------------------------------------------------------
-- 1. 基础角色与劳动安排
-- ---------------------------------------------------------------------
INSERT INTO sys_role (role_code, role_name) VALUES
    ('SUPER_ADMIN', '超级管理员'),
    ('TEACHER', '教师'),
    ('ASSISTANT', '助教')
ON DUPLICATE KEY UPDATE role_name = VALUES(role_name), deleted = 0;

INSERT INTO company (name, sort_order, status) VALUES
    ('茶园', 1, 1),
    ('果园', 2, 1)
ON DUPLICATE KEY UPDATE sort_order = VALUES(sort_order), status = 1, deleted = 0;

SET @tea_company_id := (SELECT id FROM company WHERE name = '茶园' AND deleted = 0 LIMIT 1);
SET @fruit_company_id := (SELECT id FROM company WHERE name = '果园' AND deleted = 0 LIMIT 1);

INSERT INTO classes (class_name, class_code, company_id, week, start_session, end_session, academic_year, status)
VALUES
    ('茶园-第1周-1~2节', '1-1-2', @tea_company_id, 1, 1, 2, '2026-2027', 1),
    ('果园-第2周-3~4节', '2-3-4', @fruit_company_id, 2, 3, 4, '2026-2027', 1),
    ('茶园-第3周-5~6节（已归档示例）', '3-5-6', @tea_company_id, 3, 5, 6, '2026-2027', 0)
ON DUPLICATE KEY UPDATE
    class_name = VALUES(class_name), class_code = VALUES(class_code),
    academic_year = VALUES(academic_year), status = VALUES(status), deleted = 0;

SET @class_tea := (SELECT id FROM classes WHERE company_id = @tea_company_id AND week = 1 AND start_session = 1 AND end_session = 2 AND deleted = 0 LIMIT 1);
SET @class_fruit := (SELECT id FROM classes WHERE company_id = @fruit_company_id AND week = 2 AND start_session = 3 AND end_session = 4 AND deleted = 0 LIMIT 1);

-- ---------------------------------------------------------------------
-- 2. 学生（含一名助教）
-- ---------------------------------------------------------------------
INSERT INTO student (student_id, name, class_id, gender, original_major, student_no_in_class, status, is_assistant)
VALUES
    ('S2026001', '张三', @class_tea, 1, '计算机科学与技术', 1, 1, 0),
    ('S2026002', '李四', @class_tea, 1, '软件工程', 2, 1, 0),
    ('S2026003', '王芳', @class_tea, 2, '网络工程', 3, 1, 0),
    ('S2026004', '赵磊', @class_tea, 1, '数据科学与大数据技术', 4, 1, 1),
    ('S2026005', '陈静', @class_fruit, 2, '计算机科学与技术', 1, 1, 0),
    ('S2026006', '刘强', @class_fruit, 1, '软件工程', 2, 1, 0)
ON DUPLICATE KEY UPDATE
    name = VALUES(name), class_id = VALUES(class_id), gender = VALUES(gender),
    original_major = VALUES(original_major), student_no_in_class = VALUES(student_no_in_class),
    status = VALUES(status), is_assistant = VALUES(is_assistant), deleted = 0;

SET @assistant_student_id := (SELECT id FROM student WHERE student_id = 'S2026004' AND deleted = 0 LIMIT 1);
SET @student_zhangsan_id := (SELECT id FROM student WHERE student_id = 'S2026001' AND deleted = 0 LIMIT 1);
SET @student_lisi_id := (SELECT id FROM student WHERE student_id = 'S2026002' AND deleted = 0 LIMIT 1);
SET @student_wangfang_id := (SELECT id FROM student WHERE student_id = 'S2026003' AND deleted = 0 LIMIT 1);

-- ---------------------------------------------------------------------
-- 3. 登录账号与角色
-- ---------------------------------------------------------------------
-- BCrypt('cdjcc123456')；三个测试账号均按首次登录处理，登录后必须修改密码。
SET @test_password_hash := '$2a$10$lEXUo.SkavGolbwCYXzAlOQFQgnNPABcRz0APsoUBPAsdsCEqvcWa';

INSERT INTO sys_user (username, password_hash, real_name, status, first_login, last_password_change_time, student_id)
VALUES
    ('admin1', @test_password_hash, '测试管理员', 1, 1, NULL, NULL),
    ('teacher1', @test_password_hash, '张老师', 1, 1, NULL, NULL),
    ('assistant1', @test_password_hash, '赵助教', 1, 1, NULL, @assistant_student_id)
ON DUPLICATE KEY UPDATE
    password_hash = VALUES(password_hash), real_name = VALUES(real_name), status = 1,
    first_login = 1, last_password_change_time = NULL,
    student_id = VALUES(student_id), deleted = 0;

SET @admin_user_id := (SELECT id FROM sys_user WHERE username = 'admin1' AND deleted = 0 LIMIT 1);
SET @teacher_user_id := (SELECT id FROM sys_user WHERE username = 'teacher1' AND deleted = 0 LIMIT 1);
SET @assistant_user_id := (SELECT id FROM sys_user WHERE username = 'assistant1' AND deleted = 0 LIMIT 1);
SET @super_admin_role_id := (SELECT id FROM sys_role WHERE role_code = 'SUPER_ADMIN' AND deleted = 0 LIMIT 1);
SET @teacher_role_id := (SELECT id FROM sys_role WHERE role_code = 'TEACHER' AND deleted = 0 LIMIT 1);
SET @assistant_role_id := (SELECT id FROM sys_role WHERE role_code = 'ASSISTANT' AND deleted = 0 LIMIT 1);

INSERT INTO user_role (user_id, role_id) VALUES
    (@admin_user_id, @super_admin_role_id),
    (@teacher_user_id, @teacher_role_id),
    (@assistant_user_id, @assistant_role_id)
ON DUPLICATE KEY UPDATE deleted = 0;

-- 教师可操作两个班级；助教只负责茶园班。
INSERT INTO teacher_class (user_id, class_id) VALUES
    (@teacher_user_id, @class_tea),
    (@teacher_user_id, @class_fruit)
ON DUPLICATE KEY UPDATE deleted = 0;

INSERT INTO assistant_class (assistant_student_id, class_id) VALUES
    (@assistant_student_id, @class_tea)
ON DUPLICATE KEY UPDATE deleted = 0;

-- ---------------------------------------------------------------------
-- 4. 考勤、单次成绩与换证考试成绩
-- ---------------------------------------------------------------------
INSERT INTO attendance_session (class_id, week_no, session_date, is_last_session, status, created_by, updated_by)
VALUES
    (@class_tea, 1, '2026-09-14', 0, 1, @teacher_user_id, @teacher_user_id),
    (@class_tea, 2, '2026-09-21', 1, 0, @teacher_user_id, @teacher_user_id)
ON DUPLICATE KEY UPDATE
    session_date = VALUES(session_date), is_last_session = VALUES(is_last_session),
    status = VALUES(status), updated_by = VALUES(updated_by), deleted = 0;

SET @session_week1 := (SELECT id FROM attendance_session WHERE class_id = @class_tea AND week_no = 1 AND deleted = 0 LIMIT 1);

INSERT INTO attendance_record (session_id, student_id, attendance_type, score, remark, recorded_by)
VALUES
    (@session_week1, @student_zhangsan_id, 'NORMAL', 9.50, '表现良好', @teacher_user_id),
    (@session_week1, @student_lisi_id, 'J', 0.00, '事假', @teacher_user_id),
    (@session_week1, @student_wangfang_id, 'NORMAL', 8.00, '按时完成任务', @teacher_user_id)
ON DUPLICATE KEY UPDATE
    attendance_type = VALUES(attendance_type), score = VALUES(score), remark = VALUES(remark),
    recorded_by = VALUES(recorded_by), deleted = 0;

INSERT INTO certificate_score (student_id, class_id, academic_year, semester, final_score, remark, recorded_by)
VALUES
    (@student_zhangsan_id, @class_tea, '2026-2027', 1, 92.00, '换证考试通过', @teacher_user_id),
    (@student_lisi_id, @class_tea, '2026-2027', 1, 78.50, '需补充实践材料', @teacher_user_id)
ON DUPLICATE KEY UPDATE
    class_id = VALUES(class_id), final_score = VALUES(final_score), remark = VALUES(remark),
    recorded_by = VALUES(recorded_by), deleted = 0;

-- ---------------------------------------------------------------------
-- 5. 操作日志示例（重新执行不会累积重复测试日志）
-- 附带主键范围条件，以兼容 MySQL Workbench 的安全更新模式（Error 1175）。
-- ---------------------------------------------------------------------
DELETE FROM operation_log
WHERE id > 0 AND description LIKE '[测试数据]%';

INSERT INTO operation_log
    (operator_user_id, module_name, operation_type, target_type, target_id, description, before_data, after_data, client_ip)
VALUES
    (@teacher_user_id, 'ATTENDANCE', 'UPDATE', 'ATTENDANCE_RECORD', @student_zhangsan_id,
     '[测试数据] 登记张三第1周考勤与单次成绩', NULL,
     JSON_OBJECT('attendanceType', 'NORMAL', 'score', 9.50), '127.0.0.1'),
    (@teacher_user_id, 'CERTIFICATE_SCORE', 'CREATE', 'STUDENT', @student_zhangsan_id,
     '[测试数据] 录入张三换证考试成绩', NULL,
     JSON_OBJECT('academicYear', '2026-2027', 'semester', 1, 'finalScore', 92.00), '127.0.0.1');

COMMIT;

-- =====================================================================
-- 可直接登录的小程序测试账号（密码均为 cdjcc123456）：
-- admin1     超级管理员：用户、导入、操作日志等管理功能
-- teacher1   教师：负责“茶园-第1周-1~2节”“果园-第2周-3~4节”
-- assistant1 助教：关联学生 S2026004，只负责“茶园-第1周-1~2节”
-- =====================================================================
