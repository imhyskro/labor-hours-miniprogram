# 劳动学时管理系统 API 接口文档（前端可接入版）

> 更新时间：2026-09-15  
> 依据：当前后端源码静态核对结果  
> 范围：只列出当前可以接入或可按现有权限接入的接口；已知存在问题的接口列在文末，暂不接入。

## 1. 通用约定

### 1.1 基础地址

本地开发地址：

```text
http://localhost:8080
```

接口统一使用 `/api` 前缀，例如：

```text
POST http://localhost:8080/api/auth/login
```

### 1.2 请求头

登录接口不需要 Token。其余接口均需携带登录返回的 JWT：

```http
Authorization: Bearer <token>
Content-Type: application/json
```

上传 Excel 时使用 `multipart/form-data`，不要手动固定 `Content-Type` 的 boundary。

### 1.3 角色编码

| 角色编码 | 含义 |
| --- | --- |
| `SUPER_ADMIN` | 超级管理员 |
| `TEACHER` | 教师 |
| `ASSISTANT` | 助教 |

### 1.4 普通响应格式

除 Excel 文件下载接口外，响应体统一为：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": null,
  "timestamp": 1789440000000
}
```

常用 `code`：

| code | 含义 | 前端处理建议 |
| --- | --- | --- |
| `200` | 成功 | 使用 `data` |
| `400` | 参数错误 | 展示 `message` |
| `401` | 未登录、Token 无效或登录失败 | 清除 Token，跳转登录页 |
| `403` | 无权限 | 提示无权限，不重复请求 |
| `500` | 系统异常 | 提示稍后重试 |
| `1000` | 业务异常 | 直接展示 `message` |
| `1002` | 无权操作该班级 | 提示数据不在负责范围内 |

**注意：前端不能只检查 HTTP 状态。部分业务异常的 HTTP 状态可能仍为 200，必须继续判断 `response.data.code === 200`。**

### 1.5 分页响应

分页数据位于 `data` 中，前端主要使用：

```json
{
  "records": [],
  "total": 0,
  "size": 10,
  "current": 1,
  "pages": 0
}
```

---

## 2. 可接入接口总览

| 模块 | 方法 | 路径 | 可用角色 | 说明 |
| --- | --- | --- | --- | --- |
| 认证 | POST | `/api/auth/login` | 公开 | 登录 |
| 认证 | POST | `/api/auth/change-password` | 已登录用户 | 修改本人密码 |
| 公司 | GET | `/api/companies` | 超级管理员 | 公司列表 |
| 公司 | POST | `/api/companies` | 超级管理员 | 新增公司 |
| 公司 | PUT | `/api/companies/{id}` | 超级管理员 | 公司改名 |
| 公司 | DELETE | `/api/companies/{id}` | 超级管理员 | 删除公司 |
| 班级 | GET | `/api/classes/page` | 管理员、教师 | 班级分页 |
| 班级 | GET | `/api/classes/list` | 管理员、教师 | 班级下拉列表 |
| 班级 | GET | `/api/classes/company/{companyId}` | 管理员、教师 | 公司下班级 |
| 班级 | GET | `/api/classes/{classId}/students` | 管理员、教师 | 班级学生 |
| 班级 | GET | `/api/classes/{id}` | 管理员、教师 | 班级详情 |
| 班级 | POST | `/api/classes` | 超级管理员 | 新增班级 |
| 班级 | PUT | `/api/classes/{id}` | 超级管理员 | 修改班级 |
| 班级 | DELETE | `/api/classes/{id}` | 超级管理员 | 删除班级 |
| 总表 | GET | `/api/master-list` | 管理员、教师 | 学生与助教总表 |
| 学生 | GET | `/api/students/page` | 管理员、教师 | 学生分页 |
| 学生 | GET | `/api/students/{id}` | 管理员、教师 | 学生基础详情 |
| 学生 | DELETE | `/api/students/{id}` | 管理员、教师 | 删除学生 |
| 用户 | GET | `/api/admin/users/page` | 超级管理员 | 用户分页 |
| 用户 | GET | `/api/admin/users/{userId}` | 超级管理员 | 用户详情 |
| 用户 | POST | `/api/admin/users` | 超级管理员 | 创建教师账号 |
| 用户 | PUT | `/api/admin/users/{userId}/status` | 超级管理员 | 启用或停用账号 |
| 用户 | PUT | `/api/admin/users/{userId}/reset-password` | 超级管理员 | 重置密码 |
| 用户 | GET | `/api/admin/users/{userId}/classes` | 超级管理员 | 查询教师负责班级 |
| 用户 | PUT | `/api/admin/users/{userId}/classes` | 超级管理员 | 设置教师负责班级 |
| 导入 | GET | `/api/import/template/students` | 超级管理员 | 下载学生模板 |
| 导入 | GET | `/api/import/template/assistants` | 超级管理员 | 下载助教模板 |
| 导入 | POST | `/api/import/students` | 超级管理员 | 导入学生 |
| 导入 | POST | `/api/import/assistants` | 超级管理员 | 导入助教 |

教师调用班级、学生、总表查询时，后端会自动限制为该教师负责的班级；前端不需要传教师 ID。

---

## 3. 认证接口

### 3.1 登录

```http
POST /api/auth/login
```

权限：公开。

请求体：

```json
{
  "username": "teacher1",
  "password": "用户密码"
}
```

成功响应中的 `data`：

```json
{
  "token": "eyJ...",
  "userInfo": {
    "id": 2,
    "username": "teacher1",
    "realName": "张老师",
    "roles": ["TEACHER"]
  },
  "firstLogin": false
}
```

前端处理：

1. 保存 `token`。
2. 保存 `userInfo` 和 `roles`，用于菜单显示。
3. `firstLogin=true` 时立即跳转修改密码页。
4. 登录失败通常返回 HTTP 401，响应体 `code=401`。

### 3.2 修改本人密码

```http
POST /api/auth/change-password
```

权限：任意已登录用户。

请求体：

```json
{
  "oldPassword": "原密码",
  "newPassword": "NewPass123!",
  "confirmPassword": "NewPass123!"
}
```

新密码要求：不少于 8 位，并且在大写字母、小写字母、数字、特殊字符 `@$!%*?&` 四类中至少包含三类。

成功后 `firstLogin` 会被置为 false。前端应清除旧 Token并让用户重新登录。

---

## 4. 公司管理接口

本模块所有接口仅 `SUPER_ADMIN` 可用，教师端不要请求。

### 4.1 查询公司列表

```http
GET /api/companies
```

`data` 示例：

```json
[
  {
    "id": 1,
    "name": "茶园",
    "sortOrder": 1,
    "status": 1,
    "classCount": 2,
    "createdAt": "2026-09-15T09:00:00"
  }
]
```

### 4.2 新增公司

```http
POST /api/companies
```

```json
{
  "name": "茶园"
}
```

公司名称不能为空且不能重复。

### 4.3 公司改名

```http
PUT /api/companies/{id}
```

```json
{
  "name": "新公司名称"
}
```

### 4.4 删除公司

```http
DELETE /api/companies/{id}
```

该公司下仍有班级时不能删除。

---

## 5. 班级接口

查询接口允许 `SUPER_ADMIN`、`TEACHER`；教师只能获得自己负责的班级。新增、修改、删除目前仅 `SUPER_ADMIN` 可用。

### 5.1 分页查询班级

```http
GET /api/classes/page?page=1&size=10
```

参数：

| 参数 | 必填 | 默认值 | 说明 |
| --- | --- | --- | --- |
| `page` | 否 | 1 | 页码 |
| `size` | 否 | 10 | 每页数量 |

`records` 中的班级结构：

```json
{
  "id": 1,
  "className": "茶园-第1周-1~2节",
  "classCode": "1-1-2",
  "companyId": 1,
  "companyName": "茶园",
  "week": 1,
  "startSession": 1,
  "endSession": 2,
  "academicYear": "24-25",
  "status": 1,
  "studentCount": 20,
  "createdAt": "2026-09-15T09:00:00"
}
```

### 5.2 查询班级下拉列表

```http
GET /api/classes/list
```

返回 `ClassVO[]`，适用于选择班级的下拉框。

### 5.3 查询指定公司的班级

```http
GET /api/classes/company/{companyId}
```

返回 `ClassVO[]`。教师仍只能看到自己负责的班级。

### 5.4 查询班级学生

```http
GET /api/classes/{classId}/students
```

`data` 示例：

```json
[
  {
    "id": 10,
    "studentId": "S2026001",
    "name": "张三",
    "studentNoInClass": 1,
    "fullNo": "1-1-2-1",
    "originalMajor": "软件工程",
    "gender": 1,
    "isAssistant": 0
  }
]
```

枚举：`gender` 为 0 未知、1 男、2 女；`isAssistant` 为 0 学生、1 助教。

### 5.5 查询班级详情

```http
GET /api/classes/{id}
```

返回单个 `ClassVO`。

### 5.6 新增班级

```http
POST /api/classes
```

仅超级管理员。

```json
{
  "companyId": 1,
  "week": 1,
  "startSession": 1,
  "endSession": 2,
  "academicYear": "24-25"
}
```

`academicYear` 可不传，后端默认 `24-25`。同一公司不能存在周次和节次完全相同的班级。

### 5.7 修改班级

```http
PUT /api/classes/{id}
```

仅超级管理员。请求体中的 `id` 不需要传，以路径参数为准。

```json
{
  "week": 2,
  "startSession": 3,
  "endSession": 4,
  "status": 1
}
```

### 5.8 删除班级

```http
DELETE /api/classes/{id}
```

仅超级管理员。班级下仍有学生时不能删除。

---

## 6. 学生与助教总表

```http
GET /api/master-list
```

权限：`SUPER_ADMIN`、`TEACHER`。教师自动限制为负责班级。

查询参数：

| 参数 | 必填 | 默认值 | 说明 |
| --- | --- | --- | --- |
| `page` | 否 | 1 | 页码 |
| `size` | 否 | 10 | 每页数量 |
| `keyword` | 否 | 无 | 学号或姓名模糊搜索 |
| `companyId` | 否 | 无 | 公司 ID |
| `classId` | 否 | 无 | 班级 ID |
| `identity` | 否 | 无 | `STUDENT` 或 `ASSISTANT` |

示例：

```http
GET /api/master-list?page=1&size=10&keyword=张&companyId=1&identity=STUDENT
```

`records` 中的数据结构：

```json
{
  "id": 10,
  "studentId": "S2026001",
  "name": "张三",
  "gender": 1,
  "classId": 1,
  "companyId": 1,
  "companyName": "茶园",
  "week": 1,
  "startSession": 1,
  "endSession": 2,
  "classCode": "1-1-2",
  "className": "茶园-第1周-1~2节",
  "studentNoInClass": 1,
  "fullNo": "1-1-2-1",
  "originalMajor": "软件工程",
  "isAssistant": 0,
  "identity": "学生",
  "assignedClassNames": null,
  "hasAccount": 0
}
```

总表页面建议使用本接口，不要用 `/api/students/page` 拼接完整编号和助教信息。

---

## 7. 当前可用的学生接口

权限：`SUPER_ADMIN`、`TEACHER`。教师只能查询或删除负责班级内的学生。

### 7.1 学生分页

```http
GET /api/students/page?page=1&size=10&keyword=张&classId=1
```

| 参数 | 必填 | 默认值 | 说明 |
| --- | --- | --- | --- |
| `page` | 否 | 1 | 页码 |
| `size` | 否 | 10 | 每页数量 |
| `keyword` | 否 | 无 | 学号或姓名模糊搜索 |
| `classId` | 否 | 无 | 班级 ID |

`records` 字段：`id`、`studentId`、`name`、`classId`、`className`、`gender`、`status`、`createdAt`。

### 7.2 学生基础详情

```http
GET /api/students/{id}
```

返回字段与学生分页记录一致。需要完整编号、原始专业或助教信息时，应使用总表接口。

### 7.3 删除学生

```http
DELETE /api/students/{id}
```

删除助教学生时，后端会同时清理其助教班级关联和登录账号。

`POST /api/students` 和 `PUT /api/students/{id}` 暂缓接入，原因见文末。

---

## 8. 管理员用户接口

本模块所有接口仅 `SUPER_ADMIN` 可用。

### 8.1 用户分页

```http
GET /api/admin/users/page?page=1&size=10&keyword=teacher&status=1
```

| 参数 | 必填 | 默认值 | 说明 |
| --- | --- | --- | --- |
| `page` | 否 | 1 | 页码 |
| `size` | 否 | 10 | 每页数量 |
| `keyword` | 否 | 无 | 用户名或真实姓名 |
| `status` | 否 | 无 | 1 启用、0 停用 |

`records` 示例：

```json
{
  "id": 2,
  "username": "teacher1",
  "realName": "张老师",
  "roles": ["TEACHER"],
  "status": 1,
  "firstLogin": true,
  "lastPasswordChangeTime": "2026-09-15T09:00:00",
  "createdAt": "2026-09-15T09:00:00"
}
```

### 8.2 用户详情

```http
GET /api/admin/users/{userId}
```

返回单个 `UserVO`。

### 8.3 创建教师账号

```http
POST /api/admin/users
```

```json
{
  "username": "teacher2",
  "realName": "李老师"
}
```

成功时 `data`：

```json
{
  "userId": 3
}
```

后端自动分配 `TEACHER` 角色并设置首次登录标志。前端不要提交密码字段。

### 8.4 启用或停用账号

```http
PUT /api/admin/users/{userId}/status?status=0
```

`status`：1 启用，0 停用。管理员不能停用自己的账号。

### 8.5 重置密码

```http
PUT /api/admin/users/{userId}/reset-password
```

重置后 `firstLogin=true`，用户下次登录必须修改密码。管理员不能重置自己的账号。

### 8.6 查询教师负责班级

```http
GET /api/admin/users/{userId}/classes
```

返回 `ClassVO[]`。

### 8.7 设置教师负责班级

```http
PUT /api/admin/users/{userId}/classes
```

该操作是全量覆盖，不是追加。

```json
{
  "classIds": [1, 2, 3]
}
```

传空数组表示取消该教师的全部班级分配。

---

## 9. Excel 导入接口

当前四个接口仅 `SUPER_ADMIN` 可用。管理员前端可以接入；教师端暂时不要展示导入按钮。

### 9.1 下载学生模板

```http
GET /api/import/template/students
```

响应为 `.xlsx` 二进制文件，不是 `CommonResult`。前端应设置 `responseType: 'blob'`。

模板列顺序：

```text
学号、姓名、公司名称、周次、开始节次、结束节次、班内编号、原始专业、性别(男/女)
```

### 9.2 下载助教模板

```http
GET /api/import/template/assistants
```

响应为 `.xlsx` 二进制文件，模板列顺序：

```text
学号、姓名、公司名称、周次、开始节次、结束节次、班内编号、原始专业
```

### 9.3 导入学生

```http
POST /api/import/students
Content-Type: multipart/form-data
```

表单字段名必须是 `file`。

### 9.4 导入助教

```http
POST /api/import/assistants
Content-Type: multipart/form-data
```

表单字段名必须是 `file`。

两个导入接口成功响应中的 `data` 格式：

```json
{
  "total": 10,
  "successCount": 8,
  "failCount": 2,
  "errors": [
    {
      "row": 3,
      "studentId": "S2026001",
      "reason": "学号已存在"
    }
  ]
}
```

接口整体返回成功不代表每一行都导入成功，前端必须展示 `failCount` 和 `errors`。

---

## 10. 前端接入关键规则

1. 登录成功后保存 Token，后续请求统一加 `Authorization: Bearer <token>`。
2. 同时判断 HTTP 状态与响应体 `code`。
3. 登录返回 `firstLogin=true` 时跳转修改密码页。
4. 根据 `userInfo.roles` 控制菜单，但安全判断最终以后端 403 为准。
5. 教师无需向后端提交教师 ID；后端根据 Token 自动进行班级数据隔离。
6. 下载模板必须使用 Blob 方式处理，不能按 JSON 解析。
7. `PUT /api/admin/users/{userId}/classes` 是全量覆盖，提交前应让用户确认。

---

## 11. 暂缓接入的接口

以下接口虽然已有路由，但当前存在已知问题，不应作为稳定接口交给前端：

| 接口 | 暂缓原因 |
| --- | --- |
| `POST /api/students` | 尚未完整接收班内编号、原始专业，且未校验同班班内编号重复 |
| `PUT /api/students/{id}` | 尚未校验同班班内编号重复 |
| `/api/assistants/**` 全部接口 | 教师权限已放开，但服务层尚未按照教师负责班级进行数据隔离，存在越权风险 |
| 助教考勤与打分接口 | 当前后端尚无对应 Controller，暂不存在可接入接口 |

另外，教师目前不能访问 `/api/companies`，教师页面如需公司筛选，应从已授权的班级数据中提取公司信息，或等待后端提供教师可读的公司列表接口。

