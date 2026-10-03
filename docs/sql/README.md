# 数据库结构与初始化说明

本目录定义劳动学时管理系统的数据库设计基线，包括建库建表、基础配置和结构检查脚本。数据库用于保存用户权限、学年学期、公司与班级、学生修读、助教分配、课次、考勤、成绩、审批、导入导出、通知、日志和统计汇总数据。

数据库名称为：

```text
labor_management_v2
```

## 环境要求

- MySQL 8.0 或兼容版本
- 存储引擎：InnoDB
- 字符集：`utf8mb4`
- 排序规则：`utf8mb4_unicode_ci`
- 执行账号具有创建、删除数据库及创建表、索引、外键的权限

## 文件职责与执行顺序

| 顺序 | 文件 | 作用 | 是否修改数据 |
| --- | --- | --- | --- |
| 1 | `00_rebuild_schema.sql` | 删除并重新建立 `labor_management_v2`，创建全部表、索引和约束 | 是，具有破坏性 |
| 2 | `01_seed_reference.sql` | 写入角色、业务默认值及导入模板字段定义 | 是，可重复执行 |
| 3 | `02_verify_schema.sql` | 检查数据库、表、外键、索引和基础数据 | 否，只读检查 |

执行 `00_rebuild_schema.sql` 会删除 `labor_management_v2` 中的全部数据。该脚本适合建立开发或测试基线，不应直接用于覆盖包含有效业务数据的生产数据库。生产环境结构升级应另行编写版本化迁移脚本。

后端资源目录 `backend/src/main/resources/db/v2` 保存同一组脚本的部署副本。修改本目录脚本后必须同步对应副本，避免开发文档与发布包使用不同数据库结构。

## 业务概念与关系

### 学年学期、公司和班级

- `academic_term` 保存学年学期，是班级、学生修读和业务汇总的时间范围。
- `company` 保存果园、桃园、一公司、二公司等业务大类。
- `teaching_group` 是系统内部的班级实体，在同一学年学期、同一公司内由“周次+连续课节”唯一确定，例如 `周1-1-2`。
- 一个班级只有一名当前任课教师；`teacher_assignment_history` 保存任课教师发生变化前后的历史。
- `course_session` 表示该班级在某个具体日期发生的一次课，由当天连续两个课节组成。

### 学生、助教和修读

- `student` 保存跨学期稳定的学生主档。
- `administrative_class` 保存学生原始行政班名称。
- `student_course_enrollment` 保存学生某学期的修读关系、所属班级、班内编号和导入快照。
- 学生通常只在一个学期完成一次普通修读；需要再次学习时建立 `RETAKE` 重修记录，不覆盖原记录。
- 助教首先是学生，通过 `assistant_profile` 建立助教身份和登录账号关系。
- 助教可被分配至多个班级，一个班级也可有多个助教，关系保存在 `assistant_group_assignment`。

### 考勤、分数和修改审批

- `attendance_record` 保存某次课中学生的出勤状态。
- `score_record` 保存当前有效分数。
- `score_revision` 保存每次分数修改前后的不可变历史。
- `change_request` 保存超期或封存后的修改申请。
- `approval_action` 保存审批过程中的每一步处理意见。
- 前端置灰只是操作提示，后端仍需校验角色、班级范围、课次状态和修改授权。

### 导入、导出、通知、日志和汇总

- `import_template_definition`、`import_template_column` 定义导入模板及字段。
- `data_import_batch`、`data_import_error` 保存导入批次和逐项错误。
- `export_job` 保存导出中心的异步任务状态和文件信息。
- `notification`、`notification_recipient` 保存通知内容及接收状态。
- `operation_log` 保存关键操作及修改前后值。
- `student_course_summary`、`teaching_group_summary` 保存统计汇总，避免报表长期扫描大量事实记录。

## 表模块总览

| 模块 | 数据表 |
| --- | --- |
| 学期与组织 | `academic_term`、`company`、`administrative_class`、`teaching_group`、`teacher_assignment_history` |
| 用户权限 | `sys_user`、`sys_role`、`user_role` |
| 学生与助教 | `student`、`student_course_enrollment`、`assistant_profile`、`assistant_group_assignment` |
| 课次考勤与评分 | `course_session`、`attendance_record`、`score_record`、`score_revision` |
| 申请审批 | `change_request`、`approval_action` |
| 系统配置与通知 | `business_config`、`notification`、`notification_recipient` |
| 导入导出 | `import_template_definition`、`import_template_column`、`data_import_batch`、`data_import_error`、`export_job` |
| 审计与汇总 | `operation_log`、`student_course_summary`、`teaching_group_summary` |

## Excel 导入契约

学生导入和助教导入使用相同的六个必填表头，默认顺序如下：

| 列号 | 表头 | 数据库去向 | 规则 |
| --- | --- | --- | --- |
| 1 | 公司 | `company`、`teaching_group` 及修读快照 | 必须匹配已有公司名称 |
| 2 | 节次 | `student_course_enrollment.section_value` | 格式为“周次-开始课节-结束课节-学生班内编号” |
| 3 | 学号 | `student.student_no` | 学生唯一标识；助教导入也先建立或更新学生主档 |
| 4 | 姓名 | `student.student_name` | 去除首尾空格后不可为空 |
| 5 | 性别 | `student.gender` | 只允许“男”“女”“未知” |
| 6 | 行政班 | `administrative_class` | 保存业务方原始名称 |

班级没有额外的人为编码。在某一学年学期、某一公司范围内，班级由周次和连续课节唯一确定。例如：

```text
1-1-2-17
```

- `1-1-2` 定位班级 `周1-1-2`；
- `17` 是该学生在班级内的编号；
- “节次”是业务方沿用的导入列名，不等同于 `course_session` 所表示的具体上课日期。

Excel 不增加“学年”“学期”列。用户上传文件前必须在页面选择学年学期，接口必须携带 `termId`；缺少 `termId` 时应在解析文件前拒绝请求。

助教不维护第二份人员档案。助教导入先建立或更新行政班、学生和修读关系，再建立助教身份、账号和班级分配，因此姓名、性别和行政班只维护一份。

## 导入校验与错误提示

应用层在写入业务表前必须验证整份文件。每条错误至少包含：

```text
rowNo、columnNo、columnName、errorCode、message、rawValue
```

| 场景 | `errorCode` | 提示示例 |
| --- | --- | --- |
| 缺少整个表头 | `MISSING_COLUMN` | `缺少“行政班”列` |
| 表头重复 | `DUPLICATE_COLUMN` | `第6列表头“行政班”重复` |
| 单元格为空 | `EMPTY_CELL` | `第8行“学号”列为空` |
| 性别值错误 | `INVALID_GENDER` | `第8行“性别”值“其他”无效，仅允许男、女、未知` |
| 节次格式错误 | `INVALID_SECTION` | `第8行“节次”格式错误，应为“周次-开始课节-结束课节-学生班内编号”，例如1-1-2-17` |
| 周次错误 | `INVALID_WEEK` | `第8行“节次”中的周次必须为1至5` |
| 课节错误 | `INVALID_PERIOD` | `第8行“节次”中的课节必须为1-2、3-4、5-6或7-8` |
| 班内编号错误 | `INVALID_SECTION_NO` | `第8行“节次”中的学生班内编号必须为正整数` |
| 公司不存在 | `COMPANY_NOT_FOUND` | `第8行“公司”值“果圆”不存在` |
| 班级不存在 | `GROUP_NOT_FOUND` | `第8行“节次”对应的班级“周1-1-2”不存在` |
| 文件内学号重复 | `DUPLICATE_STUDENT_NO` | `第8行“学号”与第3行重复` |
| 班内编号重复 | `DUPLICATE_SECTION_NO` | `第8行“节次”与第5行在同一班级中使用了相同学生编号` |
| 已有普通修读 | `NORMAL_ENROLLMENT_EXISTS` | `第8行学生已有普通修读记录，如需再次修读请走重修流程` |
| 助教账号冲突 | `ASSISTANT_ACCOUNT_CONFLICT` | `第8行学号对应账号已存在且角色不允许转为助教` |

系统应按表头名称匹配字段，同时校验六个字段是否完整。若还要求固定列顺序，应先报告缺失字段，再报告“第 X 列应为某字段”，不能只返回笼统的“模板错误”。

## 导入事务规则

1. 创建导入批次，状态为 `UPLOADED`。
2. 一次性收集缺列、重复列及表头顺序错误。
3. 逐行检查必填值、格式、公司、班级、重复数据和修读唯一性。
4. 只要存在一条错误，保存全部错误，将批次设为 `REJECTED`，业务表写入数量必须为 0。
5. 全部校验通过后，在一个事务中写入行政班、学生和修读关系；助教导入还需写入账号、角色、助教身份和班级分配。
6. 任意业务写入失败时回滚该批次全部业务数据，并将批次标记为 `ROLLED_BACK` 或 `FAILED`。

## 数据完整性原则

- 主键使用 `BIGINT UNSIGNED AUTO_INCREMENT`。
- 业务唯一性同时由应用校验和数据库唯一约束保护。
- 外键列建立索引，避免关联查询和约束检查产生不必要的全表扫描。
- 教师当前分配与教师历史分开保存，兼顾读取效率和责任追溯。
- 基础档案允许按确认后的业务规则物理删除；已经形成的关键事实记录保存名称、编号等快照。
- 事实表外键按业务影响使用 `RESTRICT` 或 `SET NULL`，不使用级联删除清空历史。
- 高频统计读取汇总表，原始事实表仍作为可追溯依据。
- 超期、封存和临时修改权限使用配置及审批数据控制，不在代码中硬编码固定天数。

## 与后端的集成边界

数据库脚本只定义数据结构和基础配置，不会自动改造 Java 代码。后端接入本数据库时，需要同步处理：

- Entity 与字段映射；
- Mapper 查询和唯一性判断；
- Service 事务、权限和状态校验；
- DTO、VO 与接口参数；
- 导入模板解析和错误返回；
- 操作日志、通知、导出和汇总任务。

建议按以下依赖顺序接入：

```text
用户权限
→ 学年学期、公司与班级
→ 学生及助教导入
→ 课次、考勤与分数
→ 修改申请与审批
→ 通知、导出与操作日志
→ 统计汇总任务
```

在后端完成相应模块迁移前，应让既有实现继续使用与其表结构匹配的数据库，不能仅更换连接地址后直接运行。

## 修改与检查要求

每次修改数据库基线后，应至少检查：

1. 三个脚本能否在空环境按顺序执行；
2. 表、索引、外键和基础配置是否通过 `02_verify_schema.sql`；
3. 导入模板字段与页面、接口、错误码是否一致；
4. 受影响的后端代码和文档是否已列入迁移范围；
5. `docs/sql` 与 `backend/src/main/resources/db/v2` 的同名 SQL 文件是否一致。
