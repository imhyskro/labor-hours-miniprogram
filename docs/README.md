# 劳动学时管理系统

高校劳动学时管理系统，用于教师和助教管理学生劳动实践、考勤、补课、成绩和重修等业务。

业务规则基准见：[PROJECT_RULES.md](./PROJECT_RULES.md)

---

## 一、项目结构

```
劳动v1/
├── backend/                # 后端 Spring Boot 服务
├── frontend/               # 学生/助教端 uni-app 前端
├── admin/                  # 教师管理端 Vue3 + Element Plus
└── docs/                   # 项目文档
    ├── PROJECT_RULES.md    # 业务规则基准
    ├── DEVELOPMENT_PHASES.md  # 开发阶段记录
    ├── README.md           # 项目说明（本文件）
    └── sql/                # 数据库 SQL 脚本
```

### 后端分层结构

```
backend/src/main/java/com/labor/management/
├── controller/             # 控制层
├── service/                # 业务接口
│   └── impl/               # 业务实现
├── mapper/                 # 数据访问层
├── entity/                 # 数据库实体
├── dto/                    # 入参对象
├── vo/                     # 出参对象
├── enums/                  # 枚举
├── config/                 # 配置类
├── security/               # 安全相关
├── exception/              # 异常处理
├── common/                 # 通用组件
└── util/                   # 工具类
```

### 后端资源结构

```
backend/src/main/resources/
├── mapper/                 # MyBatis XML 映射
├── db/                     # 数据库初始化脚本
└── application.yml         # 主配置
```

---

## 二、技术栈

### 后端

| 技术           | 版本      |
| -------------- | --------- |
| Java           | 21        |
| Spring Boot    | 3.2.5     |
| Maven          | 3.9.x     |
| MySQL          | 8.0       |
| MyBatis-Plus    | 3.5.5     |
| Lombok         | 由 Spring Boot 管理 |
| Jakarta Validation | 由 Spring Boot 管理 |
| Spring Security | 由 Spring Boot 管理 |
| JWT (jjwt)     | 0.12.5    |

### 前端（学生/助教端）

| 技术       | 版本     |
| ---------- | -------- |
| uni-app    | 3.0.0-alpha (vue3) |
| Vue        | 3.4.21   |
| TypeScript | 5.4.5    |
| Pinia      | 2.1.7    |
| Vite       | 5.2.8    |

### 教师管理端

| 技术           | 版本     |
| -------------- | -------- |
| Vue            | 3.4.21   |
| TypeScript     | 5.4.5    |
| Vite           | 5.2.8    |
| Element Plus   | 2.7.2    |
| Pinia          | 2.1.7    |
| Vue Router     | 4.3.0    |
| Axios         | 1.6.8    |

### 数据库

- 字符集：utf8mb4
- 排序规则：utf8mb4_unicode_ci
- 存储引擎：InnoDB

---

## 三、启动方式

### 1. 后端启动

数据库配置位置：[backend/src/main/resources/application.yml](../backend/src/main/resources/application.yml)

需要先修改以下配置项以匹配本地 MySQL：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/labor_management?...
    username: root
    password: 123456
```

创建数据库：

```sql
CREATE DATABASE labor_management
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;
```

启动命令：

```bash
cd backend
mvn spring-boot:run
```

或使用 IDE 直接运行 `LaborManagementApplication` 主类。

后端服务默认端口：`8080`，接口前缀：`/api`。

### 2. 前端（学生/助教端）启动

```bash
cd frontend
npm install         # 首次需要安装依赖
npm run dev:h5      # H5 开发模式
```

- H5 开发地址：http://localhost:5173
- 微信小程序：`npm run dev:mp-weixin`，然后用微信开发者工具打开 `dist/dev/mp-weixin`

### 3. 教师管理端启动

```bash
cd admin
npm install         # 首次需要安装依赖
npm run dev
```

- 开发地址：http://localhost:5174
- 已配置 `/api` 代理到后端 `http://localhost:8080`

---

## 四、开发阶段

详细阶段记录见：[DEVELOPMENT_PHASES.md](./DEVELOPMENT_PHASES.md)

当前状态：阶段0（项目初始化）开发中。

---

## 五、环境要求

| 工具   | 最低版本 |
| ------ | -------- |
| JDK    | 21       |
| Maven  | 3.8+     |
| Node   | 18+      |
| MySQL  | 8.0      |
