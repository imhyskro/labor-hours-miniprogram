-- ============================================================================
-- 文件：01_seed_reference.sql
-- 作用：向已建好的 V2 数据库写入系统运行必需的基础字典和默认配置。
-- 前置：必须先执行 00_rebuild_schema.sql。
-- 是否删除数据：否；但当前脚本按“空的新库”编写，不应在同一库重复执行。
-- 写入内容：
--   1. 超级管理员、教师、助教三种角色；
--   2. 打分期限、审批窗口、导入上限、导出保留时间等默认配置；
--   3. 学生导入和助教导入两套模板定义；
--   4. 两套模板共同使用的六个字段：公司、节次、学号、姓名、性别、行政班。
-- 本文件不创建具体账号、学生、班级或考勤测试数据。
-- ============================================================================
SET NAMES utf8mb4;
USE labor_management_v2;
-- 使用事务保证基础数据要么全部写入，要么全部回滚。
START TRANSACTION;

-- 系统角色代码会被后端权限判断使用，不建议随意修改 role_code。
INSERT INTO sys_role (role_code, role_name) VALUES
    ('ADMIN', '超级管理员'),
    ('TEACHER', '教师'),
    ('ASSISTANT', '助教');

-- 可调整业务参数。服务层读取这些值，不应把10天等规则硬编码在 Java 中。
INSERT INTO business_config (config_key, config_value, value_type, description) VALUES
    ('score.edit_deadline_days', '10', 'INTEGER', '课次创建后允许直接打分和首次修改的默认天数'),
    ('score.approved_edit_window_hours', '24', 'INTEGER', '修改申请获批后的临时修改窗口小时数'),
    ('teacher.cross_group_operation_enabled', 'true', 'BOOLEAN', '教师是否可跨教学分组操作'),
    ('import.max_rows', '10000', 'INTEGER', '单次导入最大数据行数'),
    ('export.file_expire_hours', '72', 'INTEGER', '导出中心文件保留小时数');

-- 建立两个独立模板版本，便于未来学生与助教模板分别升级。
INSERT INTO import_template_definition
    (template_code, template_name, template_version, status)
VALUES
    ('STUDENT_IMPORT', '学生导入模板', 1, 'ACTIVE'),
    ('ASSISTANT_IMPORT', '助教导入模板', 1, 'ACTIVE');

-- 取得刚写入的模板主键，供下面的列定义使用。
SET @student_template_id = (
    SELECT id FROM import_template_definition
    WHERE template_code='STUDENT_IMPORT' AND template_version=1
);
SET @assistant_template_id = (
    SELECT id FROM import_template_definition
    WHERE template_code='ASSISTANT_IMPORT' AND template_version=1
);

-- 两类导入现在使用完全相同的六个中文表头和排列顺序。
-- validation_hint 是后端校验和错误提示的规则来源，不直接代替 Java 校验。
INSERT INTO import_template_column
    (template_id, column_no, column_name, field_code, required_flag, value_type, max_length, validation_hint)
VALUES
    (@student_template_id, 1, '公司',   'companyName',             TRUE, 'TEXT',    100, '必须与公司主档名称完全一致'),
    (@student_template_id, 2, '节次',   'sectionValue',            TRUE, 'SECTION',  80, '格式：周次-开始课节-结束课节-学生班内编号，例如1-1-2-17'),
    (@student_template_id, 3, '学号',   'studentNo',               TRUE, 'TEXT',     50, '同一文件及学生主档中均不可重复'),
    (@student_template_id, 4, '姓名',   'studentName',             TRUE, 'TEXT',    100, '去除首尾空格后不可为空'),
    (@student_template_id, 5, '性别',   'gender',                  TRUE, 'GENDER',   10, '仅允许男、女、未知'),
    (@student_template_id, 6, '行政班', 'administrativeClassName', TRUE, 'TEXT',    100, '保留原始行政班名称'),
    (@assistant_template_id, 1, '公司',   'companyName',             TRUE, 'TEXT',    100, '必须与公司主档名称完全一致'),
    (@assistant_template_id, 2, '节次',   'sectionValue',            TRUE, 'SECTION',  80, '格式：周次-开始课节-结束课节-学生班内编号，例如1-1-2-17'),
    (@assistant_template_id, 3, '学号',   'studentNo',               TRUE, 'TEXT',     50, '建议同时作为助教账号用户名'),
    (@assistant_template_id, 4, '姓名',   'studentName',             TRUE, 'TEXT',    100, '去除首尾空格后不可为空'),
    (@assistant_template_id, 5, '性别',   'gender',                  TRUE, 'GENDER',   10, '仅允许男、女、未知'),
    (@assistant_template_id, 6, '行政班', 'administrativeClassName', TRUE, 'TEXT',    100, '保留原始行政班名称');

COMMIT;

-- 执行结束后返回模板列清单，方便人工确认两套模板是否都是六列。
SELECT d.template_code, c.column_no, c.column_name, c.field_code, c.validation_hint
FROM import_template_definition d
JOIN import_template_column c ON c.template_id=d.id
ORDER BY d.template_code, c.column_no;
