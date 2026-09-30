-- ============================================================================
-- 文件：02_verify_schema.sql
-- 作用：在执行完前两个脚本后，人工检查 V2 数据库是否建立完整。
-- 是否修改数据：否。本文件只有 SELECT，可以重复执行。
-- 检查内容：
--   1. V2 基础表总数；
--   2. 每张表的名称和中文用途；
--   3. 学生、助教导入模板是否都包含六个必填字段；
--   4. 默认业务配置是否成功写入；
--   5. 外键引用以及删除、更新规则。
-- 注意：结构检查通过不代表现有 Java 后端已经适配 V2。
-- ============================================================================
SET NAMES utf8mb4;
USE labor_management_v2;

-- 结果应为 29；如果少于29，说明建表脚本中途失败。
SELECT COUNT(*) AS table_count
FROM information_schema.tables
WHERE table_schema='labor_management_v2' AND table_type='BASE TABLE';

-- 查看29张表及其 COMMENT，便于确认各表业务用途。
SELECT table_name, table_comment
FROM information_schema.tables
WHERE table_schema='labor_management_v2' AND table_type='BASE TABLE'
ORDER BY table_name;

-- 两行结果应分别为 STUDENT_IMPORT、ASSISTANT_IMPORT，字段均为：
-- 公司、节次、学号、姓名、性别、行政班。
SELECT d.template_code,
       GROUP_CONCAT(c.column_name ORDER BY c.column_no SEPARATOR '、') AS required_columns
FROM import_template_definition d
JOIN import_template_column c ON c.template_id=d.id
WHERE d.status='ACTIVE' AND c.required_flag=TRUE
GROUP BY d.id, d.template_code
ORDER BY d.template_code;

-- 核对默认配置值。之后可由管理功能修改，但不应直接散落在业务代码中。
SELECT config_key, config_value, value_type
FROM business_config
ORDER BY config_key;

-- 检查所有外键。delete_rule 用于确认物理删除时是 RESTRICT、SET NULL 还是 CASCADE。
SELECT rc.table_name, rc.constraint_name, rc.referenced_table_name,
       rc.delete_rule, rc.update_rule
FROM information_schema.referential_constraints rc
WHERE rc.constraint_schema='labor_management_v2'
ORDER BY rc.table_name, rc.constraint_name;
