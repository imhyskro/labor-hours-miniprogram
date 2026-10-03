# 劳动学时管理系统

劳动学时管理系统用于维护学生劳动课程的班级、助教、课次、考勤、评分、审批、导入导出和操作记录。系统采用前后端分离结构，业务数据统一由后端接口和 MySQL 数据库管理。本版本交付 Spring Boot 后端、助教微信小程序和项目文档，不包含 Web 管理前端。

## 目录结构

```text
labor-hours-miniprogram/
├── backend/        # Spring Boot 后端服务
├── labour-course/  # 助教微信原生小程序
├── docs/           # 业务规则、开发规划和数据库设计文档
├── .gitignore      # Git 排除规则
└── README.md       # 项目入口说明
```

### `backend`

后端负责身份认证、权限校验、业务处理、数据库访问、文件导入导出和操作日志。

- Java 21
- Spring Boot 3.2.5
- Maven
- MyBatis-Plus
- Spring Security 与 JWT
- MySQL 8.0
- 默认服务端口：`8080`
- 接口统一前缀：`/api`
- 启动类：`backend/src/main/java/com/labor/management/LaborManagementApplication.java`

本机运行配置保存于：

```text
backend/src/main/resources/application.yml
```

该文件包含数据库密码、JWT 密钥等本地信息，不应上传公开仓库。可提交的字段示例保存在：

```text
backend/src/main/resources/application-example.yml
```

### `labour-course`

助教端采用微信原生小程序结构，无需执行 `npm install`。微信开发者工具应直接导入该目录。

```text
labour-course/
├── app.js
├── app.json
├── app.wxss
├── pages/
├── utils/
├── images/
├── project.config.json
└── sitemap.json
```

后端请求地址在 `labour-course/utils/api.js` 中配置。模拟器可以访问 `http://localhost:8080`；真机不能把 `localhost` 当作开发电脑，必须使用电脑在同一局域网内的 IPv4 地址，例如：

```javascript
const BASE_URL = 'http://192.168.1.10:8080'
```

### `docs`

文档目录用于保存业务规则、阶段计划、数据库结构和需要随代码共同维护的技术说明。阅读顺序和维护要求见 [docs/README.md](docs/README.md)。

## 数据库

数据库设计基线位于 `docs/sql`，数据库名称为：

```text
labor_management_v2
```

首次建立数据库时按顺序执行：

1. `docs/sql/00_rebuild_schema.sql`
2. `docs/sql/01_seed_reference.sql`
3. `docs/sql/02_verify_schema.sql`

`00_rebuild_schema.sql` 会删除并重新创建 `labor_management_v2`。执行前必须确认该库中没有需要保留的数据。字段含义、导入约束和表关系说明见 [docs/sql/README.md](docs/sql/README.md)。

数据库结构与 Java 后端需要按模块完成适配。在实体类、Mapper、Service 和接口尚未迁移前，不应仅修改数据库连接并直接让既有后端连接新库。

## 本地运行

### 1. 准备环境

- JDK 21
- Maven 3.9 或 IDEA 内置 Maven
- MySQL 8.0
- 微信开发者工具

### 2. 配置数据库

复制 `application-example.yml` 的字段结构并创建本机 `application.yml`，填写数据库地址、用户名、密码和 JWT 配置。不要把真实密钥写入示例文件。

### 3. 启动后端

在 IDEA 中打开 `backend/pom.xml`，等待 Maven 完成依赖解析，然后运行：

```text
com.labor.management.LaborManagementApplication
```

也可以在 PowerShell 中运行：

```powershell
cd E:\labor-hours-miniprogram-c\labor-hours-miniprogram\backend
mvn spring-boot:run
```

后端启动成功时应能看到类似日志：

```text
Tomcat started on port 8080
Started LaborManagementApplication
```

### 4. 打开微信小程序

在微信开发者工具中导入：

```text
E:\labor-hours-miniprogram-c\labor-hours-miniprogram\labour-course
```

本地开发阶段需要确认：

- 测试 AppID 可用，登录微信号具有调试权限；
- `project.config.json` 中的 `urlCheck` 为 `false`；
- 后端已经启动且接口地址正确；
- 真机与电脑处于可互访的同一网络；
- Windows 防火墙允许 Java 或 TCP 8080 端口入站；
- 手机浏览器能够访问 `http://电脑局域网IP:8080`。

## 联调顺序

1. 建立数据库并执行基础数据脚本。
2. 核对 `application.yml` 的数据库连接。
3. 启动后端并确认 8080 端口可访问。
4. 使用接口工具验证登录接口及测试账号。
5. 配置前端请求地址。
6. 在模拟器中检查请求地址、请求参数和响应内容。
7. 在真机环境检查局域网、防火墙和微信域名校验配置。

出现“网络请求失败”时，应先看开发者工具 Network 面板：没有 HTTP 状态码且请求长时间超时，通常是网络、IP 或防火墙问题；已经得到 4xx/5xx 状态码，则说明请求到达后端，应继续检查权限、参数、业务异常和数据库日志。

## 版本管理

应提交：

- 后端和小程序源码；
- `pom.xml`、前端依赖清单及锁文件；
- 数据库结构脚本和必要的初始化脚本；
- 不含密钥的配置示例；
- 与代码一致的需求、接口和运行文档。

不应提交：

- `node_modules/`
- `target/`
- `.m2repo/`
- `.idea/`、`.vscode/`、`.trae/` 等个人工具配置
- `project.private.config.json`
- 日志、缓存和临时转换文件
- 含真实数据库密码或 JWT 密钥的 `application.yml`
- 仓库外 `E:\labor-hours-miniprogram-c\temp` 中的归档内容

## 开发约束

- 业务含义以评审确认的需求和规则文档为准，不根据页面名称自行推测。
- 前端隐藏按钮不能代替后端权限校验。
- 数据库结构、导入模板、接口字段和页面展示字段发生变化时，相关文档必须同步更新。
- 核心事实数据和操作记录应保留历史；允许物理删除的基础档案也必须避免破坏已经形成的业务关系。
- 修改数据库基线脚本后，应同步维护后端资源目录 `backend/src/main/resources/db/v2` 中的部署副本。
