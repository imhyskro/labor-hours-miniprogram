-- =====================================================================
-- 修复：同一班级中，未删除学生的“班内编号”必须唯一
-- 适用：已经建好 labor_management_test 测试库，不希望重新初始化全部数据
-- 说明：本脚本不会自动修改重复学生的编号，避免误改真实数据。
-- 正式库上线时，将下面的 USE 改为 USE labor_management; 后再执行。
-- =====================================================================

USE labor_management_test;

-- 1. 执行后必须返回 0 行，才能继续执行第 3 步。
--    若有结果，表示同一 class_id 下存在重复的 student_no_in_class。
SELECT
    class_id,
    student_no_in_class,
    COUNT(*) AS duplicate_count,
    GROUP_CONCAT(student_id ORDER BY id SEPARATOR ', ') AS student_ids
FROM student
WHERE deleted = 0
  AND class_id IS NOT NULL
  AND student_no_in_class IS NOT NULL
GROUP BY class_id, student_no_in_class
HAVING COUNT(*) > 1;

-- 2. 如果上一步有结果，先查询重复记录并人工确定正确编号。
--    将下面的 class_id 和 student_no_in_class 替换成上一步返回的值。
-- SELECT id, student_id, name, class_id, student_no_in_class
-- FROM student
-- WHERE deleted = 0
--   AND class_id = 1
--   AND student_no_in_class = 1
-- ORDER BY id;
--
-- 确认后只修改应调整的那一条，例如：
-- UPDATE student
-- SET student_no_in_class = 3
-- WHERE student_id = 'A2026001';
--
-- 修改后重新执行第 1 步，直到返回 0 行。

-- 3. 将普通索引改为唯一保护。
--    生成列在 deleted=1 时为 NULL，使逻辑删除后的旧记录不占用编号。
ALTER TABLE student
    DROP INDEX idx_student_no_in_class,
    ADD COLUMN active_student_no_in_class INT
        GENERATED ALWAYS AS (
            CASE WHEN deleted = 0 THEN student_no_in_class ELSE NULL END
        ) STORED COMMENT '仅用于约束未删除学生的班内编号唯一',
    ADD UNIQUE KEY uk_student_active_class_no
        (class_id, active_student_no_in_class);

-- 4. 验证列和唯一索引是否创建成功。
SHOW COLUMNS FROM student LIKE 'active_student_no_in_class';
SHOW INDEX FROM student WHERE Key_name = 'uk_student_active_class_no';

-- 5. 可选检查：当前 W-S-E-N 完整编号在不同公司之间是否重复。
--    这里仅检查，不自动修改。若业务要求全系统唯一，需要另行确定公司编号规则。
SELECT
    CONCAT(c.week, '-', c.start_session, '-', c.end_session, '-', s.student_no_in_class) AS full_no,
    COUNT(*) AS duplicate_count,
    GROUP_CONCAT(CONCAT(co.name, ':', s.student_id) ORDER BY co.id, s.id SEPARATOR ', ') AS records
FROM student s
JOIN classes c ON c.id = s.class_id AND c.deleted = 0
JOIN company co ON co.id = c.company_id AND co.deleted = 0
WHERE s.deleted = 0
  AND s.student_no_in_class IS NOT NULL
GROUP BY full_no
HAVING COUNT(*) > 1;
