// 后端 API 封装：依据 f/API接口文档-前端可接入.md
// ====== 切换开关：true = 使用本地 Mock 数据（无需后端），false = 调用真实后端 ======
const USE_MOCK = false

const auth = require('./auth.js')

// 本地开发地址，按需修改
const BASE_URL = 'http://192.168.32.22:8080'

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
  if (token) header['Authorization'] = 'Bearer ' + token
  // GET 请求无需设置 Content-Type，wx.request 自带 application/json;charset=UTF-8
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
            // 登录页本身收到 401 时不再跳转，直接 reject 让页面自行提示
            const pages = getCurrentPages()
            const currentPath = pages.length ? '/' + pages[pages.length - 1].route : ''
            if (currentPath !== '/pages/login/login') {
              wx.showToast({ title: body.message || '登录已失效，请重新登录', icon: 'none' })
              setTimeout(() => {
                wx.reLaunch({ url: '/pages/login/login' })
              }, 800)
            }
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
    return request('POST', '/api/auth/login', { data: { username, password } })
  },

  // 修改本人密码
  changePassword(oldPassword, newPassword, confirmPassword) {
    return request('POST', '/api/auth/change-password', {
      data: { oldPassword, newPassword, confirmPassword }
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
    return request('GET', '/api/attendance/classes')
  },
  // 查询班级课次
  listAttendanceSessions(classId) {
    return request('GET', '/api/attendance/sessions' + toQuery({ classId }))
  },
  // 新建课次
  createAttendanceSession(body) {
    return request('POST', '/api/attendance/sessions', { data: body })
  },
  // 修改或封存课次
  updateAttendanceSession(sessionId, body) {
    return request('PUT', '/api/attendance/sessions/' + sessionId, { data: body })
  },
  // 查询全班考勤与单次分数
  listAttendanceRecords(sessionId) {
    return request('GET', '/api/attendance/sessions/' + sessionId + '/records')
  },
  // 导出课次考勤 Excel
  exportAttendanceRecords(sessionId) {
    return request('GET', '/api/attendance/sessions/' + sessionId + '/export', {
      responseType: 'arraybuffer',
      raw: true
    })
  },
  // 登记或覆盖单个学生考勤与本次分数
  saveAttendanceRecord(sessionId, studentId, body) {
    return request('PUT', '/api/attendance/sessions/' + sessionId + '/records/' + studentId, { data: body })
  },
  // 删除单个学生考勤记录
  deleteAttendanceRecord(sessionId, studentId) {
    return request('DELETE', '/api/attendance/sessions/' + sessionId + '/records/' + studentId)
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

// ====== Mock 覆盖：USE_MOCK=true 时所有接口走本地假数据，无需后端 ======
if (USE_MOCK) {
  module.exports = require('./api.mock.js')
}
