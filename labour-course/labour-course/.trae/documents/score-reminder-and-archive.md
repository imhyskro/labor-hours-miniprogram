# 助教打分提醒与自动封存机制

## Context

助教容易遗漏打分任务，历史积压又会造成打扰。需要一个"10天窗口提醒 + 超期自动封存"的闭环：
- 登录后检查过去10天内是否有未打分课次，有则弹窗提醒
- 超过10天的未打分记录视为已封存，不再提醒
- 新建打分列表页展示待处理课次

后端是 Java Spring Boot + MySQL + MyBatis-Plus（`com.labor.management` 包），前端是原生微信小程序。

## 实现方案

### 一、后端（Java Spring Boot）

#### 1. 新增 ResultCode 枚举值
文件：`common/ResultCode.java`
```java
PENDING_SCORE(201, "检测到您近10天内有未打分的课程，请尽快处理"),
```

#### 2. 新增 PendingScoreVO
文件：`vo/PendingScoreVO.java`
字段：`sessionId, classId, className, weekNo, sessionDate, unscoredCount`

#### 3. 新增 ScoreService 接口 + 实现
文件：`service/ScoreService.java`、`service/impl/ScoreServiceImpl.java`

**checkToday() 逻辑**：
1. `SecurityUtil.getCurrentUserId()` 取当前用户
2. `ClassAccessService.getAttendanceClassIds()` 取可访问班级 ID 列表（null=管理员全部）
3. 用 `AttendanceSessionMapper` 查 `sessionDate BETWEEN today.minusDays(9) AND today` 且 `status = 1`（可编辑/未封存）的课次
4. 对每个课次，用 `AttendanceRecordMapper` 查该 `sessionId` 下 `score IS NOT NULL` 的记录数，与班级学生总数比较；若存在未打分学生，收集到 pending 列表
5. pending 非空 → `new CommonResult<>(201, msg, pendingList)`；空 → `CommonResult.success(null)`

**listPending() 逻辑**：与 checkToday 相同查询，但始终返回 `CommonResult.success(pendingList)`（code 200）

**"未打分"判定**：课次对应的班级中存在学生，其 `attendance_record` 无记录或 `score` 为 null。实现时复用 `AttendanceRecordMapper`（BaseMapper）做 count 查询，`StudentMapper` 查班级学生总数。

#### 4. 新增 ScoreController
文件：`controller/ScoreController.java`
```java
@RestController
@RequestMapping("/api/score")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('SUPER_ADMIN', 'TEACHER', 'ASSISTANT')")
public class ScoreController {
    private final ScoreService scoreService;

    @GetMapping("/check-today")
    public CommonResult<List<PendingScoreVO>> checkToday() {
        return scoreService.checkToday();
    }

    @GetMapping("/pending")
    public CommonResult<List<PendingScoreVO>> listPending() {
        return scoreService.listPending();
    }
}
```

#### 5. 定时封存任务（建议，本轮不实现）
超10天课次由 `check-today` 的日期过滤自然排除，无需物理标记即可满足"不再提醒"。后续可加 `@Scheduled` 任务将 `status` 改为 0（封存）优化查询性能。

### 二、前端（小程序）

#### 1. api.js 新增两个函数
文件：`utils/api.js` module.exports 中追加：
```javascript
// 检查今日待打分（返回 code 201 表示有待办）
checkTodayScore() {
  return request('GET', '/api/score/check-today', { raw: true })
},
// 获取待打分课次列表
listPendingScores() {
  return request('GET', '/api/score/pending')
},
```
> `raw: true` 绕过 request() 的 code===200 解包逻辑，让调用方手动检查 `res.data.code`，避免 201 被当作错误 reject。

#### 2. login.js 登录成功后插入待办检查
文件：`pages/login/login.js`，在 `api.login().then(data => {...})` 内、`firstLogin` 分支之后、`else`（非首次登录）分支中：

```javascript
// 非首次登录：先进入首页，再异步检查待打分
wx.reLaunch({
  url: role.path,
  success: () => this.checkPendingScore()
})
```

新增 `checkPendingScore()` 方法：
```javascript
checkPendingScore() {
  api.checkTodayScore().then(res => {
    const body = res.data
    if (body && body.code === 201) {
      wx.showModal({
        title: '⚠️ 待办提醒',
        content: '检测到您近10天内有未打分的课程，请尽快处理！',
        confirmText: '去打分',
        cancelText: '稍后',
        confirmColor: '#4A6B3A',
        success: r => {
          if (r.confirm) {
            wx.navigateTo({ url: '/pages/score/list?status=pending' })
          }
        }
      })
    }
  }).catch(() => {
    // 接口失败/超时，不阻塞用户
  })
}
```

#### 3. 新建 pages/score/list 页面

**app.json**：pages 数组追加 `"pages/score/list"`

**list.json**：
```json
{ "navigationBarTitleText": "待打分课程" }
```

**list.wxml**：复用 login/attendance 的背景+毛玻璃卡片风格。列表项展示班级名、课次日期、周次、未打分人数，点击跳转考勤页。

**list.wxss**：复用 wheat-field 背景 + glass-card 毛玻璃 + weave-btn 深绿织布按钮，与现有页面统一。

**list.js**：
- `onLoad(options)`：读取 `status` 参数，调 `api.listPendingScores()` 加载列表
- 列表项点击：`wx.navigateTo` 到 `/pages/attendance/attendance?classId=xx&className=xx`

## 关键文件清单

| 层 | 文件 | 操作 |
|---|---|---|
| 后端 | `common/ResultCode.java` | 新增 PENDING_SCORE(201) |
| 后端 | `vo/PendingScoreVO.java` | 新建 |
| 后端 | `service/ScoreService.java` | 新建接口 |
| 后端 | `service/impl/ScoreServiceImpl.java` | 新建实现 |
| 后端 | `controller/ScoreController.java` | 新建 |
| 前端 | `utils/api.js` | 追加 2 个函数 |
| 前端 | `pages/login/login.js` | 新增 checkPendingScore + 调用 |
| 前端 | `pages/score/list.{wxml,wxss,js,json}` | 新建 4 文件 |
| 前端 | `app.json` | 追加页面注册 |

## 验证方式

1. **后端**：启动 Spring Boot，带有效 JWT 调 `GET /api/score/check-today`，验证有未打分课次时返回 `code:201`，无时返回 `code:200`
2. **前端登录**：登录非首次登录账号，验证有 pending 时弹 modal；点"去打分"进 score/list 页；点列表项进考勤页
3. **异常**：后端未启动时登录不阻塞，直接进首页
