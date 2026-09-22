USE labor_management;
-- 班级编码 W-S-E 在不同公司间会重复，去掉 class_code 全局唯一约束
ALTER TABLE classes DROP INDEX uk_classes_class_code;
-- 改为 公司+周次+开始节次+结束节次 联合唯一
ALTER TABLE classes ADD UNIQUE KEY uk_classes_company_session (company_id, week, start_session, end_session);
