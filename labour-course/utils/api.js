// 后端 API 封装。接口基准以仓库 docs 和最新 backend Controller 为准。
const auth = require('./auth.js')

// 本地开发地址，按需修改
const BASE_URL = 'http://localhost:8080'

// 本地无后端时开启 Mock：true=使用假数据不发起真实请求；false=走真实后端
const USE_MOCK = false

function parseLocalDate(dateText) {
  if (!dateText) return null
  const parts = String(dateText).split('-').map(Number)
  if (parts.length !== 3 || parts.some(n => !Number.isFinite(n))) return null
  const date = new Date(parts[0], parts[1] - 1, parts[2])
  return isNaN(date.getTime()) ? null : date
}

// 小程序按本机当前日期复核打分窗口；后端会使用同一 sealDays 再做强制校验。
function isSessionEditableNow(session) {
  if (!session) return false
  if (typeof session.editable === 'boolean') return session.editable
  const start = parseLocalDate(session.sessionDate)
  if (!start) return false
  const now = new Date()
  const today = new Date(now.getFullYear(), now.getMonth(), now.getDate())
  const sealDays = Math.max(1, Number(session.sealDays) || 10)
  const sealDate = new Date(start.getFullYear(), start.getMonth(), start.getDate() + sealDays)
  return today >= start && today < sealDate
}

/**
 * 通用请求：自动加 Token、统一处理业务 code
 * @param {string} method GET / POST / PUT / DELETE
 * @param {string} path  以 /api 开头
 * @param {object} opts
 *  - data:        query (GET) 或 body (POST/PUT)
 *  - header:      额外头
 *  - responseType: wx.request 的 responseType
 *  - raw:         为 true 时直接返回 wx.request 的 res，不做业务 code 解析（用于 blob 下载等）
 */
function request(method, path, opts = {}) {
  const token = auth.getToken()
  const header = Object.assign({}, opts.header || {})
  const isLoginRequest =
    path === '/api/auth/login' ||
    path === '/api/auth/mini-login'

  if (token && !isLoginRequest) {
    header['Authorization'] = 'Bearer ' + token
  }
  if (method !== 'GET' && !header['Content-Type']) {
    header['Content-Type'] = 'application/json'
  }

  return new Promise((resolve, reject) => {
    wx.request({
      url: BASE_URL + path,
      method,
      data: opts.data,
      header,
      responseType: opts.responseType || 'text',
      dataType: opts.dataType || 'json',
      success: res => {
        if (opts.raw) {
          // 原始响应：HTTP 非 2xx 直接 reject
          if (res.statusCode >= 200 && res.statusCode < 300) {
            resolve(res)
          } else {
            reject(buildErr(res))
          }
          return
        }
        // 普通响应：业务 code 200 才视为成功
        const body = res.data
        if (body && typeof body === 'object' && 'code' in body) {
          if (body.code === 200) {
            resolve(body.data)
            return
          }
          // 401 未登录 / Token 失效
          if (body.code === 401) {
            auth.clearAuth()
            wx.showToast({ title: body.message || '登录已失效，请重新登录', icon: 'none' })
            setTimeout(() => {
              wx.reLaunch({ url: '/pages/login/login' })
            }, 800)
            reject(buildErr(res, body))
            return
          }
          // 其他业务异常
          reject(buildErr(res, body))
          return
        }
        // 兼容无 CommonResult 包装的 2xx 响应
        if (res.statusCode >= 200 && res.statusCode < 300) {
          resolve(body)
        } else {
          reject(buildErr(res))
        }
      },
      fail: err => {
        // 网络层失败
        const e = new Error('网络请求失败：' + (err && err.errMsg ? err.errMsg : ''))
        e.cause = err
        reject(e)
      }
    })
  })
}

function buildErr(res, body) {
  const e = new Error((body && body.message) || res.errMsg || ('HTTP ' + res.statusCode))
  e.statusCode = res.statusCode
  e.code = body && body.code
  e.body = body
  e.raw = res
  return e
}

function toQuery(params) {
  if (!params) return ''
  const parts = []
  Object.keys(params).forEach(k => {
    const v = params[k]
    if (v === undefined || v === null || v === '') return
    parts.push(encodeURIComponent(k) + '=' + encodeURIComponent(v))
  })
  return parts.length ? '?' + parts.join('&') : ''
}

// ========== 认证 ==========
module.exports = {
  // 登录
  login(username, password) {
    if (USE_MOCK) return mockLogin(username, password)
    return request('POST', '/api/auth/mini-login', { data: { username, password } })
  },

  // 修改本人密码
  changePassword(oldPassword, newPassword, confirmPassword) {
    return request('POST', '/api/auth/change-password', {
      data: { oldPassword, newPassword, confirmPassword }
    })
  },

  // 检查配置有效期内是否有未打分课次（前端按后端返回的 sealDays 复核）
  // 轻量检查：只拉班级 + 课次，不拉全班记录，登录后快速判断
  // 返回 raw 响应：code 201 表示有待办，code 200 表示无待办
  // Bug 修复：之前只数课次不检查打分状态，现在用 unscoredCount>0 判断真正未打完分的课次
  checkTodayScore() {
    return this.listAttendanceClasses().then(classes => {
      const classList = classes || []
      if (classList.length === 0) {
        return { data: { code: 200, message: '操作成功' } }
      }
      return Promise.all(classList.map(c =>
        this.listAttendanceSessions(c.id).catch(() => [])
      ))
    }).then(sessionsPerClass => {
      let count = 0
      sessionsPerClass.forEach(sessions => {
        (sessions || []).forEach(s => {
          if (isSessionEditableNow(s) && (s.unscoredCount || 0) > 0) count++
        })
      })
      return {
        data: {
          code: count > 0 ? 201 : 200,
          message: count > 0
            ? `检测到您有${count}个处于有效期内的课次需要处理打分，请尽快处理！`
            : '操作成功'
        }
      }
    }).catch(() => ({ data: { code: 200, message: '操作成功' } }))
  },

  // 获取有效期内的待打分课次列表
  // 逻辑：拉取助教可访问班级 → 每个班级的课次 → 按日期窗口筛选 → 统计未打分人数
  listPendingScores() {
    return this.listAttendanceClasses().then(classes => {
      const classList = classes || []
      if (classList.length === 0) return []

      // 并发拉取每个班级的课次列表
      return Promise.all(classList.map(c =>
        this.listAttendanceSessions(c.id).then(sessions => ({
          classItem: c,
          sessions: sessions || []
        })).catch(() => ({ classItem: c, sessions: [] }))
      ))
    }).then(results => {
      // 汇总所有课次，直接用后端返回的 unscoredCount，不再发 listRecords 请求
      const allSessions = []
      results.forEach(({ classItem, sessions }) => {
        sessions.forEach(s => {
          allSessions.push({
            sessionId: s.id,
            classId: s.classId || classItem.id,
            className: s.className || classItem.className || '未命名班级',
            weekNo: s.weekNo,
            sessionDate: s.sessionDate,
            status: s.status,
            sealDays: s.sealDays,
            unscoredCount: s.unscoredCount || 0
          })
        })
      })

      // 筛选：当前可编辑 + 有未打分学生
      return allSessions.filter(s => {
        return isSessionEditableNow(s) && s.unscoredCount > 0
      })
    })
  },

  // ========== 公司 ==========
  listCompanies() {
    return request('GET', '/api/companies')
  },
  createCompany(name) {
    return request('POST', '/api/companies', { data: { name } })
  },
  renameCompany(id, name) {
    return request('PUT', '/api/companies/' + id, { data: { name } })
  },
  deleteCompany(id) {
    return request('DELETE', '/api/companies/' + id)
  },

  // ========== 班级 ==========
  pageClasses(page = 1, size = 10) {
    return request('GET', '/api/classes/page' + toQuery({ page, size }))
  },
  listClasses() {
    return request('GET', '/api/classes/list')
  },
  listClassesByCompany(companyId) {
    return request('GET', '/api/classes/company/' + companyId)
  },
  getClassStudents(classId) {
    return request('GET', '/api/classes/' + classId + '/students')
  },
  getClassDetail(id) {
    return request('GET', '/api/classes/' + id)
  },
  createClass(body) {
    return request('POST', '/api/classes', { data: body })
  },
  updateClass(id, body) {
    return request('PUT', '/api/classes/' + id, { data: body })
  },
  deleteClass(id) {
    return request('DELETE', '/api/classes/' + id)
  },

  // ========== 学生与助教总表 ==========
  masterList(params) {
    return request('GET', '/api/master-list' + toQuery(params))
  },

  // ========== 学生 ==========
  pageStudents(params) {
    return request('GET', '/api/students/page' + toQuery(params))
  },
  getStudent(id) {
    return request('GET', '/api/students/' + id)
  },
  createStudent(body) {
    return request('POST', '/api/students', { data: body })
  },
  updateStudent(id, body) {
    return request('PUT', '/api/students/' + id, { data: body })
  },
  deleteStudent(id) {
    return request('DELETE', '/api/students/' + id)
  },

  // ========== 管理员用户 ==========
  pageUsers(params) {
    return request('GET', '/api/admin/users/page' + toQuery(params))
  },
  getUser(userId) {
    return request('GET', '/api/admin/users/' + userId)
  },
  createTeacher(username, realName) {
    return request('POST', '/api/admin/users', { data: { username, realName } })
  },
  setUserStatus(userId, status) {
    return request('PUT', '/api/admin/users/' + userId + '/status' + toQuery({ status }))
  },
  resetUserPassword(userId) {
    return request('PUT', '/api/admin/users/' + userId + '/reset-password')
  },
  getUserClasses(userId) {
    return request('GET', '/api/admin/users/' + userId + '/classes')
  },
  setUserClasses(userId, classIds) {
    return request('PUT', '/api/admin/users/' + userId + '/classes', { data: { classIds } })
  },

  // ========== Excel 导入 ==========
  // 下载学生模板：返回 wx.request 原始 res（responseType=arrayBuffer）
  downloadStudentTemplate() {
    return request('GET', '/api/import/template/students', {
      responseType: 'arraybuffer',
      raw: true
    })
  },
  downloadAssistantTemplate() {
    return request('GET', '/api/import/template/assistants', {
      responseType: 'arraybuffer',
      raw: true
    })
  },
  // 导入学生：使用 wx.uploadFile 上传 multipart
  importStudents(filePath) {
    return uploadFile('/api/import/students', filePath, auth.getToken())
  },
  importAssistants(filePath) {
    return uploadFile('/api/import/assistants', filePath, auth.getToken())
  },

  // ========== 学生导出（Excel 二进制）==========
  exportStudents(keyword, classId) {
    return request('GET', '/api/students/export' + toQuery({ keyword, classId }), {
      responseType: 'arraybuffer',
      raw: true
    })
  },

  // ========== 助教管理 ==========
  // 助教分页：返回 IPage<AssistantVO>
  pageAssistants(page, size, keyword) {
    return request('GET', '/api/assistants/page' + toQuery({ page, size, keyword }))
  },
  // 导出助教 Excel
  exportAssistants(keyword) {
    return request('GET', '/api/assistants/export' + toQuery({ keyword }), {
      responseType: 'arraybuffer',
      raw: true
    })
  },
  // 查询助教负责班级 ID 列表
  getAssistantClasses(studentId) {
    return request('GET', '/api/assistants/' + studentId + '/classes')
  },
  // 设置助教负责班级（全量覆盖）
  assignAssistantClasses(studentId, classIds) {
    return request('PUT', '/api/assistants/' + studentId + '/classes', { data: { classIds } })
  },
  // 取消助教身份
  revokeAssistant(studentId, notEnrolledThisTerm) {
    return request('PUT', '/api/assistants/' + studentId + '/revoke' + toQuery({ notEnrolledThisTerm }))
  },
  // 设置或取消助教身份
  setAssistantIdentity(studentId, isAssistant) {
    return request('PUT', '/api/assistants/' + studentId + '/identity', { data: { isAssistant } })
  },
  // 查询未分配班级的助教列表
  listUnassignedAssistants() {
    return request('GET', '/api/assistants/unassigned')
  },

  // ========== 考勤与单次成绩 ==========
  // 查询当前用户可访问的考勤班级
  listAttendanceClasses() {
    if (USE_MOCK) return mockAttendanceClasses()
    return request('GET', '/api/attendance/classes')
  },
  // 查询班级课次
  listAttendanceSessions(classId) {
    if (USE_MOCK) return mockListSessions(classId)
    return request('GET', '/api/attendance/sessions' + toQuery({ classId }))
  },
  // 查询全班考勤与单次分数
  listAttendanceRecords(sessionId) {
    if (USE_MOCK) return mockListRecords(sessionId)
    return request('GET', '/api/attendance/sessions/' + sessionId + '/records')
  },
  // 导出课次考勤 Excel
  exportAttendanceRecords(sessionId) {
    if (USE_MOCK) return Promise.reject(new Error('Mock 模式不支持导出 Excel'))
    return request('GET', '/api/attendance/sessions/' + sessionId + '/export', {
      responseType: 'arraybuffer',
      raw: true
    })
  },
  // 登记或覆盖单个学生考勤与本次分数
  saveAttendanceRecord(sessionId, studentId, body) {
    if (USE_MOCK) return mockSaveRecord(sessionId, studentId, body)
    return request('PUT', '/api/attendance/sessions/' + sessionId + '/records/' + studentId, { data: body })
  },
  // 一键打分：对当前课次全部或指定学生批量设置分数（后端存在则更新、不存在则插入）
  batchScore(sessionId, studentIds, score) {
    if (USE_MOCK) return mockBatchScore(sessionId, studentIds, score)
    return request('POST', '/api/attendance/sessions/' + sessionId + '/batch-score', {
      data: { studentIds, score, attendanceType: 'NORMAL', remark: '' }
    })
  },
  // 检查某班级有效期内是否有“未完全打分”的课次
  // 返回 boolean：true=有未打完分的课次（该班级需要红点）
  // Bug 修复：之前用 score=null 判断会误判 J/K 类型学生，现在用后端返回的 unscoredCount>0
  checkClassPending(classId) {
    return this.listAttendanceSessions(classId).then(sessions => {
      return (sessions || []).some(s => {
        return isSessionEditableNow(s) && (s.unscoredCount || 0) > 0
      })
    }).catch(() => false)
  },
  // 提交成绩修改申请；审批状态由后端 change_request 返回，不再写本地假状态
  submitModificationRequest(sessionId, studentId, reason) {
    if (USE_MOCK) return mockMarkNeedApproval(sessionId, studentId, 'pending')
    return request('POST', '/api/attendance/modification-requests', {
      data: { sessionId, studentId, reason }
    })
  },
  listModificationRequests(status, sessionId) {
    return request('GET', '/api/attendance/modification-requests' + toQuery({ status, sessionId }))
  },
  getModificationRequest(requestId) {
    return request('GET', '/api/attendance/modification-requests/' + requestId)
  },
  cancelModificationRequest(requestId) {
    return request('PUT', '/api/attendance/modification-requests/' + requestId + '/cancel')
  },

  // ========== 换证考试成绩 ==========
  // 查询班级换证成绩
  listCertificateScores(classId, academicYear, semester) {
    return request('GET', '/api/certificate-scores' + toQuery({ classId, academicYear, semester }))
  },
  // 导出班级换证成绩 Excel
  exportCertificateScores(classId, academicYear, semester) {
    return request('GET', '/api/certificate-scores/export' + toQuery({ classId, academicYear, semester }), {
      responseType: 'arraybuffer',
      raw: true
    })
  },
  // 登记或覆盖换证成绩
  saveCertificateScore(studentId, body) {
    return request('PUT', '/api/certificate-scores/' + studentId, { data: body })
  },
  // 删除换证成绩
  deleteCertificateScore(studentId, classId, academicYear, semester) {
    return request('DELETE', '/api/certificate-scores/' + studentId + toQuery({ classId, academicYear, semester }))
  },

  // ========== 操作日志 ==========
  // 分页查询操作日志
  pageOperationLogs(params) {
    return request('GET', '/api/operation-logs' + toQuery(params))
  },

  // 暴露原始 request 便于扩展
  _request: request,
  BASE_URL
}

// 上传文件：wx.uploadFile 需要单独走，不通过通用 request
function uploadFile(path, filePath, token) {
  const header = {}
  if (token) header['Authorization'] = 'Bearer ' + token
  return new Promise((resolve, reject) => {
    wx.uploadFile({
      url: BASE_URL + path,
      filePath,
      name: 'file',
      header,
      success: res => {
        // uploadFile 返回 data 是字符串
        let body
        try {
          body = JSON.parse(res.data)
        } catch (e) {
          reject(new Error('响应解析失败：' + res.data))
          return
        }
        if (body && body.code === 200) {
          resolve(body.data)
        } else {
          const e = new Error((body && body.message) || '导入失败')
          e.body = body
          reject(e)
        }
      },
      fail: err => {
        reject(new Error('上传失败：' + (err && err.errMsg ? err.errMsg : '')))
      }
    })
  })
}

// ===== Mock 数据（USE_MOCK=true 时使用，后端就绪后可忽略） =====
function mockLogin(username, password) {
  return new Promise((resolve, reject) => {
    setTimeout(() => {
      if (!username || !password) {
        reject(new Error('请输入账号和密码'))
        return
      }
      const isFirstLogin = password === username
      resolve({
        token: 'mock-token-' + Date.now(),
        userInfo: {
          id: 1,
          username: username,
          realName: '助教用户',
          roles: ['ASSISTANT']
        },
        firstLogin: isFirstLogin
      })
    }, 300)
  })
}

function mockAttendanceClasses() {
  return Promise.resolve([
    { id: 1, className: '劳动课A班', companyName: '农学院', week: 3, startSession: 1, endSession: 16 },
    { id: 2, className: '劳动课B班', companyName: '园艺学院', week: 4, startSession: 1, endSession: 16 }
  ])
}

// ===== 考勤 Mock：模块级内存数据，多页跳转间保持状态 =====
const mockStudents = [
  { studentId: 20230001, studentName: '张明', studentNo: '20230001', studentNoInClass: '01', grade: '2023', college: '农学院', major: '农学' },
  { studentId: 20230002, studentName: '李华', studentNo: '20230002', studentNoInClass: '02', grade: '2023', college: '农学院', major: '园艺' },
  { studentId: 20230003, studentName: '王芳', studentNo: '20230003', studentNoInClass: '03', grade: '2023', college: '植物保护学院', major: '植保' },
  { studentId: 20230004, studentName: '赵磊', studentNo: '20230004', studentNoInClass: '04', grade: '2023', college: '农学院', major: '农学' },
  { studentId: 20230005, studentName: '钱秀英', studentNo: '20230005', studentNoInClass: '05', grade: '2023', college: '园艺学院', major: '园艺' }
]
const mockSessions = [
  { id: 101, classId: 1, className: '劳动课A班', weekNo: 1, sessionDate: '2026-09-10', isLastSession: 0, status: 1 },
  { id: 102, classId: 1, className: '劳动课A班', weekNo: 2, sessionDate: '2026-09-17', isLastSession: 0, status: 1 },
  { id: 201, classId: 2, className: '劳动课B班', weekNo: 1, sessionDate: '2026-09-11', isLastSession: 1, status: 1 }
]
// 记录按 sessionId -> 学生列表快照（初始化为空，打分后填充）
const mockRecords = {}
let mockSessionIdSeq = 1000

function mockListSessions(classId) {
  const list = mockSessions.filter(s => s.classId === Number(classId))
  return Promise.resolve(list)
}

function mockCreateSession(body) {
  const session = {
    id: ++mockSessionIdSeq,
    classId: Number(body.classId),
    className: '',
    weekNo: Number(body.weekNo),
    sessionDate: body.sessionDate || '',
    isLastSession: body.isLastSession === 1 ? 1 : 0,
    status: 1
  }
  mockSessions.push(session)
  return Promise.resolve(session)
}

function mockUpdateSession(sessionId, body) {
  const s = mockSessions.find(x => x.id === Number(sessionId))
  if (!s) return Promise.reject(new Error('课次不存在'))
  if (body.weekNo !== undefined) s.weekNo = Number(body.weekNo)
  if (body.sessionDate !== undefined) s.sessionDate = body.sessionDate
  if (body.isLastSession !== undefined) s.isLastSession = body.isLastSession === 1 ? 1 : 0
  if (body.status !== undefined) s.status = body.status === 0 ? 0 : 1
  return Promise.resolve(s)
}

function mockListRecords(sessionId) {
  const sid = Number(sessionId)
  if (!mockRecords[sid]) {
    // 初始化为未登记的空记录；approvalStatus: '' 无 / 'pending' 需审批 / 'approved' 待修改
    mockRecords[sid] = mockStudents.map((st, i) => ({
      id: sid * 1000 + st.studentId,
      sessionId: sid,
      studentId: st.studentId,
      studentNo: st.studentNo,
      studentName: st.studentName,
      studentNoInClass: st.studentNoInClass,
      grade: st.grade,
      college: st.college,
      major: st.major,
      attendanceType: '',
      score: null,
      // Demo 种子：王芳已审批通过，用于展示绿色「待修改」标签与置顶排序
      approvalStatus: i === 2 ? 'approved' : '',
      remark: '',
      recordedBy: 'mock',
      updatedAt: Date.now()
    }))
  }
  return Promise.resolve(mockRecords[sid])
}

// 回写审批状态：助教提交申请置 pending；教师同意后置 approved
function mockMarkNeedApproval(sessionId, studentId, status) {
  const sid = Number(sessionId)
  if (!mockRecords[sid]) mockListRecords(sid)
  const rec = mockRecords[sid].find(r => r.studentId === Number(studentId))
  if (!rec) return Promise.reject(new Error('学生记录不存在'))
  rec.approvalStatus = status === 'approved' ? 'approved' : 'pending'
  rec.updatedAt = Date.now()
  return Promise.resolve(rec)
}

function mockSaveRecord(sessionId, studentId, body) {
  const sid = Number(sessionId)
  if (!mockRecords[sid]) mockListRecords(sid)
  const list = mockRecords[sid]
  let rec = list.find(r => r.studentId === Number(studentId))
  if (!rec) {
    rec = {
      id: sid * 1000 + Number(studentId),
      sessionId: sid,
      studentId: Number(studentId),
      studentNo: String(studentId),
      studentName: '学生' + studentId,
      studentNoInClass: '',
      grade: '',
      college: '',
      major: '',
      attendanceType: '',
      score: null,
      remark: '',
      recordedBy: 'mock',
      updatedAt: Date.now()
    }
    list.push(rec)
  }
  // 新建的兜底记录也补 approvalStatus
  if (rec.approvalStatus === undefined) rec.approvalStatus = ''
  rec.attendanceType = body.attendanceType || 'NORMAL'
  rec.score = (body.score !== undefined && body.score !== null) ? Number(body.score) : null
  rec.remark = body.remark || ''
  rec.updatedAt = Date.now()
  return Promise.resolve(rec)
}

// 一键打分：仅对分数为空的普通学生赋基础分
function mockBatchScore(sessionId, studentIds, score) {
  const sid = Number(sessionId)
  if (!mockRecords[sid]) mockListRecords(sid)
  const list = mockRecords[sid]
  const targets = studentIds && studentIds.length
    ? list.filter(r => studentIds.indexOf(r.studentId) >= 0)
    : list
  let success = 0
  targets.forEach(rec => {
    // 已有分数的同学跳过，不进行任何修改
    if (rec.score !== null && rec.score !== undefined && rec.score !== '') return
    rec.attendanceType = 'NORMAL'
    rec.score = Number(score)
    rec.updatedAt = Date.now()
    success++
  })
  return Promise.resolve({ success, score })
}

function mockDeleteRecord(sessionId, studentId) {
  const sid = Number(sessionId)
  if (mockRecords[sid]) {
  mockRecords[sid] = mockRecords[sid].filter(r => r.studentId !== Number(studentId))
  }
  return Promise.resolve(true)
}
