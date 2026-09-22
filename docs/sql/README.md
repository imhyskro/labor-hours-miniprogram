# 数据库脚本使用说明

后端当前配置连接 `labor_management_test`，请在 MySQL 中按以下顺序执行：

1. `init_schema.sql`：创建或重建测试数据库表。**会删除 `labor_management_test` 中的现有业务表和数据。**
2. `test_data_full.sql`：写入完整小程序测试数据；可重复执行，不会删除非测试数据。

测试账号密码均为 `cdjcc123456`：

| 账号 | 角色 | 用途 |
| --- | --- | --- |
| `admin1` | 超级管理员 | 用户管理、教师分班、学生/助教导入、操作日志 |
| `teacher1` | 教师 | 考勤、单次打分、换证成绩、负责班级数据 |
| `assistant1` | 助教 | 茶园班的考勤查询与登记 |

三个测试账号均设置为首次登录状态。首次使用上述初始密码登录后必须修改密码；完成修改前，数据库中的 `last_password_change_time` 保持为 `NULL`。

`phase6_*.sql`、`phase7_*.sql`、`fix_student_no_unique.sql` 是历史迁移脚本，不要在已经执行最新 `init_schema.sql` 的测试库上重复执行。旧的 `test_data_users.sql` 与 `test_data_classes_students.sql` 也是早期拆分数据，当前联调优先使用 `test_data_full.sql`。
