-- =====================================================================
-- 劳动学时管理系统 - 完整测试数据（覆盖后端全部关键逻辑分支）
-- 使用前提：已在 labor_management_test 中执行最新完整建表脚本（init_schema_test.sql）。
-- 警告：仅用于本地测试；会清空测试库现有数据，然后建立固定测试数据。
-- 本脚本可重复执行。所有测试账号初始密码：cdjcc123456
--
-- =========================== 测试账号一览 ===========================
-- 账号            密码           角色         负责/所属班级                    用途
-- admin          cdjcc123456    SUPER_ADMIN  （不限）                        全量数据，不受任何隔离限制
-- teacher1       cdjcc123456    TEACHER      茶园-第1周-1~2节、茶园-第2周-3~4节  常规教师，验证数据隔离
-- teacher2       cdjcc123456    TEACHER      果园-第1周-1~2节                常规教师，验证跨公司隔离
-- teacher_new    cdjcc123456    TEACHER      茶园-第1周-1~2节                 first_login=1，验证首次改密流程
-- teacher_none   cdjcc123456    TEACHER      （无任何负责班级）               验证 scope=空集：所有班级/学生操作被拒
-- S2026001       cdjcc123456    ASSISTANT    茶园-第1周-1~2节、茶园-第2周-3~4节  既是 S2026001 张三个人账号，又管理两个班
-- A2026001       cdjcc123456    ASSISTANT    果园-第1周-1~2节                 纯助教，验证助教视角与跨公司边界
--
-- ======================= 关键分支覆盖对照表 =======================
-- 【认证】teacher_new / S2026001 首次登录 first_login=1，应被引导改密
-- 【认证】其余账号 first_login=0，登录后直接进入
-- 【权限-403】A2026001 访问 /api/certificate-scores** → 403
-- 【权限-403】A2026001 访问 POST/PUT /api/attendance/sessions** → 403
-- 【权限-1002】teacher2 访问茶园班级 → 无权操作该班级数据
-- 【权限-1002】teacher_none 访问任意班级 → 无权（scope 空集）
-- 【权限-空集】teacher_none 查班级/学生/总表 → 均 0 条
-- 【助教排除】助教身份不出现在普通考勤、换证成绩名单中
-- 【自评拦截】直接请求给助教本人登记考勤 → 1002（详见文末）
-- 【课次-可编辑】茶园-第1周 第1周课次 status=1
-- 【课次-已封存】茶园-第1周 第2周课次 status=0 → 修改/登记/删除均返回 1001
-- 【课次-最后一次课】茶园-第1周 第3周课次 is_last_session=1
-- 【考勤-三类型】茶园-第1周 第1周课次含 NORMAL/J/K
-- 【考勤-零分边界】J/K 记录 score=0.00
-- 【考勤-未登记回显】茶园-第2周 第1周课次全部未登记
-- 【考勤-停用学生】S2026010 status=0，不出现在名单中
-- 【学生-编号占用】同班内普通学生与助教占用不同编号
-- 【成绩-未录入回显】成绩只录了部分学生
-- 【成绩-两个学期】semester=1 与 semester=2 各有数据
-- 【成绩-满分边界】final_score=100.00
-- =====================================================================

USE labor_management_test;

-- 1. 清空测试数据（当前表未建立外键约束）
TRUNCATE TABLE operation_log;
TRUNCATE TABLE certificate_score;
TRUNCATE TABLE attendance_record;
TRUNCATE TABLE attendance_session;
TRUNCATE TABLE assistant_class;
TRUNCATE TABLE teacher_class;
TRUNCATE TABLE user_role;
TRUNCATE TABLE sys_user;
TRUNCATE TABLE student;
TRUNCATE TABLE classes;
TRUNCATE TABLE company;
TRUNCATE TABLE sys_role;

-- 2. 角色
INSERT INTO sys_role (role_code, role_name) VALUES
    ('SUPER_ADMIN', '超级管理员'),
    ('TEACHER', '教师'),
    ('ASSISTANT', '助教');

SET @role_admin := (SELECT id FROM sys_role WHERE role_code = 'SUPER_ADMIN');
SET @role_teacher := (SELECT id FROM sys_role WHERE role_code = 'TEACHER');
SET @role_assistant := (SELECT id FROM sys_role WHERE role_code = 'ASSISTANT');

-- 3. 公司
-- 茶园/果园：正常公司；一公司：不分配给 teacher1，用于验证教师数据隔离；
-- 二公司：无任何班级，用于验证「新增班级」与「删除公司」。
INSERT INTO company (name, sort_order, status) VALUES
    ('茶园', 1, 1),
    ('果园', 2, 1),
    ('一公司', 3, 1),
    ('二公司', 4, 1);

SET @company_tea := (SELECT id FROM company WHERE name = '茶园');
SET @company_orchard := (SELECT id FROM company WHERE name = '果园');
SET @company_pending := (SELECT id FROM company WHERE name = '一公司');
SET @company_empty := (SELECT id FROM company WHERE name = '二公司');

-- 4. 班级
-- teacher1 负责茶园两个班级；teacher2 负责果园班级；teacher_new 负责茶园第1周；
-- teacher_none 无负责任何班级；一公司班级不分配给任何教师（验证越权 1002）。
INSERT INTO classes
    (class_name, class_code, company_id, week, start_session, end_session,
     academic_year, status)
VALUES
    ('茶园-第1周-1~2节', '1-1-2', @company_tea,     1, 1, 2, '2026-2027', 1),
    ('茶园-第2周-3~4节', '2-3-4', @company_tea,     2, 3, 4, '2026-2027', 1),
    ('果园-第1周-1~2节', '1-1-2', @company_orchard, 1, 1, 2, '2026-2027', 1),
    ('一公司-第3周-5~6节', '3-5-6', @company_pending, 3, 5, 6, '2026-2027', 1);

SET @class_tea_1 := (
    SELECT id FROM classes
    WHERE company_id = @company_tea AND week = 1 AND start_session = 1 AND end_session = 2
);
SET @class_tea_2 := (
    SELECT id FROM classes
    WHERE company_id = @company_tea AND week = 2 AND start_session = 3 AND end_session = 4
);
SET @class_orchard_1 := (
    SELECT id FROM classes
    WHERE company_id = @company_orchard AND week = 1 AND start_session = 1 AND end_session = 2
);
SET @class_pending_1 := (
    SELECT id FROM classes
    WHERE company_id = @company_pending AND week = 3 AND start_session = 5 AND end_session = 6
);

-- 5. 学生和助教身份
-- 事实表中的学生信息顺序：节次 / 学号 / 姓名 / 性别 / 行政班。
-- 当前数据库采用规范化结构：节次存放在 classes 表，student.class_id 关联对应节次；
-- 当前版本还没有 administrative_class 字段，因此测试阶段暂用 original_major 保存行政班。
-- student_no_in_class / status / is_assistant 是系统测试所需的附加技术字段，排在事实字段之后。
-- S2026001 张三：既是学生，也是一个助教账号（username=S2026001），管理茶园两个班。
--   其本人所属班级为茶园-第1周，但普通考勤、换证成绩名单会按 is_assistant=0 排除他。
-- S2026011 郑新：茶园-第1周普通学生，用于补齐 NORMAL/J/K 和成绩展示数据。
-- S2026010 王停用：status=0，不出现在考勤名单与成绩列表中（验证 status=1 过滤）。
-- A2026001 李助教：纯助教，class_id 为空，管理果园班级。
INSERT INTO student
    (class_id, student_id, name, gender, original_major,
     student_no_in_class, status, is_assistant)
VALUES
    (@class_tea_1,     'S2026001', '张三',   1, '2026级计算机1班',   1, 1, 1),
    (@class_tea_1,     'S2026002', '李四',   1, '2026级计算机1班',   2, 1, 0),
    (@class_tea_1,     'S2026003', '王芳',   2, '2026级计算机1班',   3, 1, 0),
    (@class_tea_2,     'S2026004', '刘强',   1, '2026级计算机2班',   1, 1, 0),
    (@class_tea_2,     'S2026005', '陈静',   2, '2026级计算机2班',   2, 1, 0),
    (@class_orchard_1, 'S2026006', '赵磊',   1, '2026级软件工程1班', 1, 1, 0),
    (@class_orchard_1, 'S2026007', '孙丽',   2, '2026级软件工程1班', 2, 1, 0),
    (@class_pending_1, 'S2026008', '周伟',   1, '2026级人工智能1班', 1, 1, 0),
    (@class_pending_1, 'S2026009', '吴敏',   2, '2026级人工智能1班', 2, 1, 0),
    (@class_tea_1,     'S2026010', '王停用', 1, '2026级计算机1班',   4, 0, 0),
    (@class_tea_1,     'S2026011', '郑新',   2, '2026级计算机1班',   5, 1, 0),
    (NULL,             'A2026001', '李助教', 2, '2026级软件工程1班', 9, 1, 1);

SET @student_1 := (SELECT id FROM student WHERE student_id = 'S2026001');
SET @student_2 := (SELECT id FROM student WHERE student_id = 'S2026002');
SET @student_3 := (SELECT id FROM student WHERE student_id = 'S2026003');
SET @student_4 := (SELECT id FROM student WHERE student_id = 'S2026004');
SET @student_5 := (SELECT id FROM student WHERE student_id = 'S2026005');
SET @student_6 := (SELECT id FROM student WHERE student_id = 'S2026006');
SET @student_11 := (SELECT id FROM student WHERE student_id = 'S2026011');
SET @assistant_student := (SELECT id FROM student WHERE student_id = 'A2026001');

-- 6. 登录账号
-- 所有账号密码均为 cdjcc123456。
-- first_login=1 的账号：teacher_new（验证首次改密）、张三（助教首登改密）。
-- 张三的 student_id 指向 S2026001，使其在考勤登记时能被后端识别为「助教本人」。
INSERT INTO sys_user
    (username, password_hash, real_name, status, first_login, student_id)
VALUES
    ('admin',
     '$2a$10$lEXUo.SkavGolbwCYXzAlOQFQgnNPABcRz0APsoUBPAsdsCEqvcWa',
     '系统管理员', 1, 0, NULL),
    ('teacher1',
     '$2a$10$lEXUo.SkavGolbwCYXzAlOQFQgnNPABcRz0APsoUBPAsdsCEqvcWa',
     '张老师', 1, 0, NULL),
    ('teacher2',
     '$2a$10$lEXUo.SkavGolbwCYXzAlOQFQgnNPABcRz0APsoUBPAsdsCEqvcWa',
     '王老师', 1, 0, NULL),
    ('teacher_new',
     '$2a$10$lEXUo.SkavGolbwCYXzAlOQFQgnNPABcRz0APsoUBPAsdsCEqvcWa',
     '新教师', 1, 1, NULL),
    ('teacher_none',
     '$2a$10$lEXUo.SkavGolbwCYXzAlOQFQgnNPABcRz0APsoUBPAsdsCEqvcWa',
     '无班级教师', 1, 0, NULL),
    ('S2026001',
     '$2a$10$lEXUo.SkavGolbwCYXzAlOQFQgnNPABcRz0APsoUBPAsdsCEqvcWa',
     '张三', 1, 1, @student_1),
    ('A2026001',
     '$2a$10$lEXUo.SkavGolbwCYXzAlOQFQgnNPABcRz0APsoUBPAsdsCEqvcWa',
     '李助教', 1, 0, @assistant_student);

SET @user_admin := (SELECT id FROM sys_user WHERE username = 'admin');
SET @user_teacher_1 := (SELECT id FROM sys_user WHERE username = 'teacher1');
SET @user_teacher_2 := (SELECT id FROM sys_user WHERE username = 'teacher2');
SET @user_teacher_new := (SELECT id FROM sys_user WHERE username = 'teacher_new');
SET @user_teacher_none := (SELECT id FROM sys_user WHERE username = 'teacher_none');
SET @user_assistant := (SELECT id FROM sys_user WHERE username = 'A2026001');
SET @user_assistant_self := (SELECT id FROM sys_user WHERE username = 'S2026001');

INSERT INTO user_role (user_id, role_id) VALUES
    (@user_admin, @role_admin),
    (@user_teacher_1, @role_teacher),
    (@user_teacher_2, @role_teacher),
    (@user_teacher_new, @role_teacher),
    (@user_teacher_none, @role_teacher),
    (@user_assistant, @role_assistant),
    (@user_assistant_self, @role_assistant);

-- 7. 教师和助教负责班级关系
INSERT INTO teacher_class (user_id, class_id) VALUES
    (@user_teacher_1, @class_tea_1),
    (@user_teacher_1, @class_tea_2),
    (@user_teacher_2, @class_orchard_1),
    (@user_teacher_new, @class_tea_1);

INSERT INTO assistant_class (assistant_student_id, class_id) VALUES
    (@student_1, @class_tea_1),
    (@student_1, @class_tea_2),
    (@assistant_student, @class_orchard_1);

-- 8. 劳动课周次
-- 茶园-第1周：第1周（可编辑）、第2周（已封存，验证 1001）、第3周（最后一次课）
-- 茶园-第2周：第1周（全部未登记，验证未登记回显）
-- 果园-第1周：第1周（纯助教管理的班级）
INSERT INTO attendance_session
    (class_id, week_no, session_date, is_last_session, status, created_by, updated_by)
VALUES
    (@class_tea_1,     1, '2026-09-07', 0, 1, @user_teacher_1, @user_teacher_1),
    (@class_tea_1,     2, '2026-09-14', 0, 0, @user_teacher_1, @user_teacher_1),
    (@class_tea_1,     3, '2026-09-21', 1, 1, @user_teacher_1, @user_teacher_1),
    (@class_tea_2,     1, '2026-09-08', 0, 1, @user_teacher_1, @user_teacher_1),
    (@class_orchard_1, 1, '2026-09-09', 0, 1, @user_teacher_2, @user_teacher_2);

SET @session_tea_1_week_1 := (
    SELECT id FROM attendance_session WHERE class_id = @class_tea_1 AND week_no = 1
);
SET @session_tea_1_week_2 := (
    SELECT id FROM attendance_session WHERE class_id = @class_tea_1 AND week_no = 2
);
SET @session_tea_1_week_3 := (
    SELECT id FROM attendance_session WHERE class_id = @class_tea_1 AND week_no = 3
);
SET @session_tea_2_week_1 := (
    SELECT id FROM attendance_session WHERE class_id = @class_tea_2 AND week_no = 1
);
SET @session_orchard_1_week_1 := (
    SELECT id FROM attendance_session WHERE class_id = @class_orchard_1 AND week_no = 1
);

-- 9. 考勤与打分记录
-- 茶园-第1周-第1周：李四/王芳/郑新已登记，覆盖 NORMAL / J / K 三种类型与 0 分边界
-- 茶园-第1周-第2周（已封存）：既有记录，验证封存后不可修改
-- 茶园-第2周-第1周：故意不插任何记录，验证「未登记学生回显 null」
-- 果园-第1周-第1周：部分登记，验证混合回显
INSERT INTO attendance_record
    (session_id, student_id, attendance_type, score, remark, recorded_by)
VALUES
    (@session_tea_1_week_1, @student_2,  'NORMAL', 5.00, NULL,       @user_teacher_1),
    (@session_tea_1_week_1, @student_3,  'J',      0.00, '请假',     @user_teacher_1),
    (@session_tea_1_week_1, @student_11, 'K',      0.00, '缺勤',     @user_teacher_1),
    (@session_tea_1_week_2, @student_2,  'NORMAL', 4.50, '表现良好', @user_teacher_1),
    (@session_tea_1_week_2, @student_3,  'NORMAL', 5.00, NULL,       @user_teacher_1),
    (@session_tea_1_week_2, @student_11, 'NORMAL', 4.00, NULL,       @user_teacher_1),
    (@session_orchard_1_week_1, @student_6, 'NORMAL', 5.00, NULL,   @user_teacher_2);

-- 10. 换证考试成绩
-- 茶园-第1周-第1周：semester=1 三人（均为普通分值），semester=2 留空对比
-- 茶园-第2周-第3~4节：semester=2 一人，final_score=100.00（满分边界）
-- 果园-第1周-第1周：semester=1 一人；其余学生留空以验证未录入回显
INSERT INTO certificate_score
    (student_id, class_id, academic_year, semester, final_score, remark, recorded_by)
VALUES
    (@student_2,  @class_tea_1,     '2026-2027', 1, 88.50, '通过',     @user_admin),
    (@student_3,  @class_tea_1,     '2026-2027', 1, 72.00, '通过',     @user_admin),
    (@student_11, @class_tea_1,     '2026-2027', 1, 55.00, '需要补考', @user_admin),
    (@student_4, @class_tea_2,     '2026-2027', 2, 100.00, '满分',    @user_admin),
    (@student_6, @class_orchard_1, '2026-2027', 1, 91.00, '通过',     @user_admin);

-- 11. 操作记录
INSERT INTO operation_log
    (operator_user_id, module_name, operation_type, target_type,
     target_id, description, before_data, after_data, client_ip)
VALUES
    (@user_admin, '用户管理', 'CREATE', 'SYS_USER', @user_teacher_1,
     '创建测试教师账号', NULL,
     JSON_OBJECT('username', 'teacher1', 'role', 'TEACHER'), '127.0.0.1'),
    (@user_teacher_1, '考勤管理', 'UPDATE', 'ATTENDANCE_RECORD', 1,
     '登记第1周考勤', NULL,
     JSON_OBJECT('class', '茶园-第1周-1~2节'), '127.0.0.1'),
    (@user_admin, '成绩管理', 'IMPORT', 'CERTIFICATE_SCORE', NULL,
     '导入换证考试测试成绩', NULL,
     JSON_OBJECT('successCount', 5), '127.0.0.1');

-- ======================= 12. 执行结果检查 =======================
SELECT 'sys_user' AS table_name, COUNT(*) AS row_count FROM sys_user WHERE deleted = 0
UNION ALL SELECT 'company', COUNT(*) FROM company WHERE deleted = 0
UNION ALL SELECT 'classes', COUNT(*) FROM classes WHERE deleted = 0
UNION ALL SELECT 'student', COUNT(*) FROM student WHERE deleted = 0
UNION ALL SELECT 'teacher_class', COUNT(*) FROM teacher_class WHERE deleted = 0
UNION ALL SELECT 'assistant_class', COUNT(*) FROM assistant_class WHERE deleted = 0
UNION ALL SELECT 'attendance_session', COUNT(*) FROM attendance_session WHERE deleted = 0
UNION ALL SELECT 'attendance_record', COUNT(*) FROM attendance_record WHERE deleted = 0
UNION ALL SELECT 'certificate_score', COUNT(*) FROM certificate_score WHERE deleted = 0
UNION ALL SELECT 'operation_log', COUNT(*) FROM operation_log;

SELECT
    u.username,
    u.real_name,
    u.first_login,
    r.role_code
FROM sys_user u
JOIN user_role ur ON ur.user_id = u.id AND ur.deleted = 0
JOIN sys_role r ON r.id = ur.role_id AND r.deleted = 0
WHERE u.deleted = 0
ORDER BY u.id;

SELECT
    u.username AS teacher_username,
    c.class_name
FROM teacher_class tc
JOIN sys_user u ON u.id = tc.user_id
JOIN classes c ON c.id = tc.class_id
WHERE tc.deleted = 0
ORDER BY u.username, c.id;

SELECT
    s.student_id AS assistant_username,
    s.name AS assistant_name,
    c.class_name
FROM assistant_class ac
JOIN student s ON s.id = ac.assistant_student_id
JOIN classes c ON c.id = ac.class_id
WHERE ac.deleted = 0
ORDER BY s.student_id, c.id;

-- =====================================================================
-- 已知无法用测试数据覆盖的分支（需通过接口参数构造，见后）
-- =====================================================================
-- 1. 助教不会出现在普通考勤名单中；直接按其 student.id 请求时仍由后端拦截。
--    当前登录助教的 sys_user.student_id == 被登记学生的 student.id。
--    本脚本中该条件已刻意留空——不构造「助教去登记自己那一行」的既有记录，
--    可手动用 S2026001 登录后直接请求给「张三」登记考勤，应返回 1002
--    「助教不能给自己登记考勤或打分」。
-- 2. 参数校验类分支（400）、封存类（1001）、越权类（1002）多数由请求构造触发，
--    数据只需保证目标对象存在即可。
--
-- 建议手动验证清单：
--   用 S2026001 登录 → 应能选择茶园两个负责班级，普通考勤名单中不出现张三
--   用 S2026001 登录 → 换证考试成绩入口不可见；直接调接口 → 403
--   用 teacher2 登录 → 看不到茶园班级；调茶园接口 → 1002
--   用 teacher_none 登录 → 班级/学生/总表均为空；任何班级操作 → 1002
--   用 admin 登录 → 修改「茶园-第1周-1~2节 第2周」课次 → 期望 1001 已封存
-- =====================================================================
