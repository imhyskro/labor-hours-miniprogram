// ========== Mock 数据版 api —— 不调用后端，返回本地假数据 ==========
// 用法：在 api.js 中设置 USE_MOCK = true 即自动切换到本模块
const auth = require('./auth.js')

// ---------- 假数据 ----------
const MOCK = {
  companies: [
    { id: 1, name: '示例公司A', sortOrder: 1, status: 1, classCount: 3, createdAt: '2026-09-01T10:00:00' },
    { id: 2, name: '示例公司B', sortOrder: 2, status: 1, classCount: 2, createdAt: '2026-09-05T14:30:00' }
  ],
  classes: [
    { id: 1, className: '2026软件1班', classCode: 'CLS001', companyId: 1, companyName: '示例公司A', week: 3, startSession: 1, endSession: 2, status: 1, studentCount: 5 },
    { id: 2, className: '2026软件2班', classCode: 'CLS002', companyId: 1, companyName: '示例公司A', week: 4, startSession: 3, endSession: 4, status: 1, studentCount: 4 },
    { id: 3, className: '2026网络1班', classCode: 'CLS003', companyId: 2, companyName: '示例公司B', week: 5, startSession: 5, endSession: 6, status: 0, studentCount: 3 }
  ],
  students: [
    { id: 1, studentId: 'S2026001', name: '张三', classId: 1, className: '2026软件1班', companyName: '示例公司A', studentNoInClass: 1, originalMajor: '软件工程', gender: 1, identity: 'STUDENT', isAssistant: false },
    { id: 2, studentId: 'S2026002', name: '李四', classId: 1, className: '2026软件1班', companyName: '示例公司A', studentNoInClass: 2, originalMajor: '软件工程', gender: 2, identity: 'STUDENT', isAssistant: false },
    { id: 3, studentId: 'S2026003', name: '王五', classId: 1, className: '2026软件1班', companyName: '示例公司A', studentNoInClass: 3, originalMajor: '软件工程', gender: 1, identity: 'ASSISTANT', isAssistant: true },
    { id: 4, studentId: 'S2026004', name: '赵六', classId: 1, className: '2026软件1班', companyName: '示例公司A', studentNoInClass: 4, originalMajor: '软件工程', gender: 2, identity: 'STUDENT', isAssistant: false },
    { id: 5, studentId: 'S2026005', name: '孙七', classId: 1, className: '2026软件1班', companyName: '示例公司A', studentNoInClass: 5, originalMajor: '软件工程', gender: 1, identity: 'STUDENT', isAssistant: false },
    { id: 6, studentId: 'S2026006', name: '周八', classId: 2, className: '2026软件2班', companyName: '示例公司A', studentNoInClass: 1, originalMajor: '网络工程', gender: 2, identity: 'STUDENT', isAssistant: false },
    { id: 7, studentId: 'S2026007', name: '吴九', classId: 2, className: '2026软件2班', companyName: '示例公司A', studentNoInClass: 2, originalMajor: '网络工程', gender: 1, identity: 'ASSISTANT', isAssistant: true },
    { id: 8, studentId: 'S2026008', name: '郑十', classId: 3, className: '2026网络1班', companyName: '示例公司B', studentNoInClass: 1, originalMajor: '网络工程', gender: 1, identity: 'STUDENT', isAssistant: false }
  ],
  users: [
    { id: 1, username: 'admin', realName: '超级管理员', roles: ['SUPER_ADMIN'], status: 1, firstLogin: false, lastPasswordChangeTime: '2026-08-01T09:00:00', createdAt: '2026-07-01T08:00:00' },
    { id: 2, username: 'teacher1', realName: '王老师', roles: ['TEACHER'], status: 1, firstLogin: false, lastPasswordChangeTime: '2026-08-15T10:00:00', createdAt: '2026-07-10T08:00:00' },
    { id: 3, username: 'teacher2', realName: '李老师', roles: ['TEACHER'], status: 0, firstLogin: false, lastPasswordChangeTime: '2026-08-20T14:00:00', createdAt: '2026-07-12T08:00:00' },
    { id: 4, username: 'assistant1', realName: '王五（助教）', roles: ['ASSISTANT'], status: 1, firstLogin: true, lastPasswordChangeTime: null, createdAt: '2026-09-01T08:00:00' }
  ],
  attendanceSessions: [
    { id: 1, classId: 1, weekNo: 1, sessionDate: '2026-09-10', isLastSession: false, status: 1, sealed: false },
    { id: 2, classId: 1, weekNo: 2, sessionDate: '2026-09-17', isLastSession: false, status: 1, sealed: false },
    { id: 3, classId: 1, weekNo: 3, sessionDate: '2026-09-24', isLastSession: true, status: 0, sealed: true }
  ],
  attendanceRecords: {
    1: [
      { id: 101, sessionId: 1, studentId: 1, studentNo: 'S2026001', studentName: '张三', studentNoInClass: 1, attendanceType: 'NORMAL', score: 8, remark: '', recordedBy: 'teacher1', updatedAt: '2026-09-10T14:00:00' },
      { id: 102, sessionId: 1, studentId: 2, studentNo: 'S2026002', studentName: '李四', studentNoInClass: 2, attendanceType: 'NORMAL', score: 9, remark: '', recordedBy: 'teacher1', updatedAt: '2026-09-10T14:00:00' },
      { id: 103, sessionId: 1, studentId: 3, studentNo: 'S2026003', studentName: '王五', studentNoInClass: 3, attendanceType: 'J', score: 0, remark: '事假', recordedBy: 'teacher1', updatedAt: '2026-09-10T14:00:00' },
      { id: 104, sessionId: 1, studentId: 4, studentNo: 'S2026004', studentName: '赵六', studentNoInClass: 4, attendanceType: 'NORMAL', score: 7, remark: '', recordedBy: 'teacher1', updatedAt: '2026-09-10T14:00:00' },
      { id: 105, sessionId: 1, studentId: 5, studentNo: 'S2026005', studentName: '孙七', studentNoInClass: 5, attendanceType: 'K', score: 0, remark: '旷课', recordedBy: 'teacher1', updatedAt: '2026-09-10T14:00:00' }
    ],
    2: [
      { id: 201, sessionId: 2, studentId: 1, studentNo: 'S2026001', studentName: '张三', studentNoInClass: 1, attendanceType: 'NORMAL', score: 9, remark: '', recordedBy: 'teacher1', updatedAt: '2026-09-17T14:00:00' },
      { id: 202, sessionId: 2, studentId: 2, studentNo: 'S2026002', studentName: '李四', studentNoInClass: 2, attendanceType: 'NORMAL', score: 10, remark: '优秀', recordedBy: 'teacher1', updatedAt: '2026-09-17T14:00:00' },
      { id: 203, sessionId: 2, studentId: 3, studentNo: 'S2026003', studentName: '王五', studentNoInClass: 3, attendanceType: 'NORMAL', score: 8, remark: '', recordedBy: 'teacher1', updatedAt: '2026-09-17T14:00:00' },
      { id: 204, sessionId: 2, studentId: 4, studentNo: 'S2026004', studentName: '赵六', studentNoInClass: 4, attendanceType: 'J', score: 0, remark: '事假', recordedBy: 'teacher1', updatedAt: '2026-09-17T14:00:00' },
      { id: 205, sessionId: 2, studentId: 5, studentNo: 'S2026005', studentName: '孙七', studentNoInClass: 5, attendanceType: 'NORMAL', score: 6, remark: '', recordedBy: 'teacher1', updatedAt: '2026-09-17T14:00:00' }
    ],
    3: [
      { id: 301, sessionId: 3, studentId: 1, studentNo: 'S2026001', studentName: '张三', studentNoInClass: 1, attendanceType: 'NORMAL', score: 10, remark: '满分', recordedBy: 'teacher1', updatedAt: '2026-09-24T14:00:00' },
      { id: 302, sessionId: 3, studentId: 2, studentNo: 'S2026002', studentName: '李四', studentNoInClass: 2, attendanceType: 'NORMAL', score: 9, remark: '', recordedBy: 'teacher1', updatedAt: '2026-09-24T14:00:00' },
      { id: 303, sessionId: 3, studentId: 3, studentNo: 'S2026003', studentName: '王五', studentNoInClass: 3, attendanceType: 'NORMAL', score: 8, remark: '', recordedBy: 'teacher1', updatedAt: '2026-09-24T14:00:00' },
      { id: 304, sessionId: 3, studentId: 4, studentNo: 'S2026004', studentName: '赵六', studentNoInClass: 4, attendanceType: 'NORMAL', score: 7, remark: '', recordedBy: 'teacher1', updatedAt: '2026-09-24T14:00:00' },
      { id: 305, sessionId: 3, studentId: 5, studentNo: 'S2026005', studentName: '孙七', studentNoInClass: 5, attendanceType: 'K', score: 0, remark: '旷课', recordedBy: 'teacher1', updatedAt: '2026-09-24T14:00:00' }
    ]
  },
  certificateScores: [
    { id: 1, studentId: 1, studentNo: 'S2026001', studentName: '张三', studentNoInClass: 1, finalScore: 85, remark: '良好' },
    { id: 2, studentId: 2, studentNo: 'S2026002', studentName: '李四', studentNoInClass: 2, finalScore: 92, remark: '' },
    { id: 3, studentId: 3, studentNo: 'S2026003', studentName: '王五', studentNoInClass: 3, finalScore: null, remark: '' },
    { id: 4, studentId: 4, studentNo: 'S2026004', studentName: '赵六', studentNoInClass: 4, finalScore: 78, remark: '' },
    { id: 5, studentId: 5, studentNo: 'S2026005', studentName: '孙七', studentNoInClass: 5, finalScore: null, remark: '缺考' }
  ],
  operationLogs: [
    { id: 1, module: '用户管理', operationType: 'CREATE', operatorName: 'admin', targetEntity: 'User', targetId: 4, description: '新增用户 assistant1', ipAddress: '127.0.0.1', createdAt: '2026-09-01T08:00:00' },
    { id: 2, module: '班级管理', operationType: 'UPDATE', operatorName: 'admin', targetEntity: 'Class', targetId: 1, description: '修改班级 2026软件1班', ipAddress: '127.0.0.1', createdAt: '2026-09-05T10:30:00' },
    { id: 3, module: '学生管理', operationType: 'IMPORT', operatorName: 'teacher1', targetEntity: 'Student', targetId: null, description: '导入学生 5 条', ipAddress: '127.0.0.1', createdAt: '2026-09-10T14:00:00' },
    { id: 4, module: '考勤管理', operationType: 'UPDATE', operatorName: 'teacher1', targetEntity: 'AttendanceRecord', targetId: 101, description: '登记考勤：张三 NORMAL 8分', ipAddress: '127.0.0.1', createdAt: '2026-09-10T14:05:00' },
    { id: 5, module: '公司管理', operationType: 'DELETE', operatorName: 'admin', targetEntity: 'Company', targetId: 3, description: '删除公司 示例公司C', ipAddress: '127.0.0.1', createdAt: '2026-09-15T09:00:00' }
  ]
}

// ---------- 辅助 ----------
function delay(ms) { return new Promise(r => setTimeout(r, ms || 200)) }

function mockArrayBuffer() {
  // 返回一个最小的有效 xlsx 签名头（PK..），让 wx.getFileSystemManager.writeFile 能写入
  const buf = new ArrayBuffer(4)
  const view = new DataView(buf)
  view.setUint8(0, 0x50) // P
  view.setUint8(1, 0x4B) // K
  view.setUint8(2, 0x03)
  view.setUint8(3, 0x04)
  return buf
}

function paginate(arr, page, size) {
  const p = page || 1
  const s = size || 50
  const start = (p - 1) * s
  return { records: arr.slice(start, start + s), total: arr.length, current: p, size: s }
}

// ---------- 导出与真实 api 完全一致的接口 ----------
module.exports = {
  BASE_URL: 'mock://localhost',

  // ========== 认证 ==========
  async login(username, password) {
    await delay(300)
    // 根据用户名返回不同角色
    let userInfo
    if (username === 'admin' || username.includes('admin')) {
      userInfo = { id: 1, username: 'admin', realName: '超级管理员', roles: ['SUPER_ADMIN'] }
    } else if (username.includes('teacher')) {
      userInfo = { id: 2, username: username, realName: '王老师', roles: ['TEACHER'] }
    } else {
      userInfo = { id: 4, username: username || 'assistant1', realName: '王五', roles: ['ASSISTANT'] }
    }
    // 检查账号是否被停用
    const mockUser = MOCK.users.find(u => u.username === username)
    if (mockUser && mockUser.status === 0) {
      const e = new Error('该账号已被停用，请联系管理员')
      e.code = 400
      throw e
    }
    // 模拟密码错误（输入 wrong 时）
    if (password === 'wrong' || password === 'error') {
      const e = new Error('账号或密码错误')
      e.code = 400
      throw e
    }
    const token = 'mock-token-' + Date.now()
    auth.saveAuth(token, userInfo)
    return { token, userInfo, firstLogin: false }
  },

  async changePassword() {
    await delay(300)
    return true
  },

  // ========== 公司 ==========
  async listCompanies() { await delay(150); return JSON.parse(JSON.stringify(MOCK.companies)) },
  async createCompany(name) { await delay(200); MOCK.companies.push({ id: Date.now(), name, sortOrder: 99, status: 1, classCount: 0, createdAt: new Date().toISOString() }); return true },
  async renameCompany(id, name) { await delay(200); const c = MOCK.companies.find(c => c.id === id); if (c) c.name = name; return true },
  async deleteCompany(id) { await delay(200); const i = MOCK.companies.findIndex(c => c.id === id); if (i >= 0) MOCK.companies.splice(i, 1); return true },

  // ========== 班级 ==========
  async pageClasses(page, size) { await delay(150); return paginate(MOCK.classes, page, size) },
  async listClasses() { await delay(150); return JSON.parse(JSON.stringify(MOCK.classes)) },
  async listClassesByCompany(companyId) { await delay(150); return MOCK.classes.filter(c => c.companyId === companyId) },
  async getClassStudents(classId) { await delay(150); return MOCK.students.filter(s => s.classId === classId).map(s => JSON.parse(JSON.stringify(s))) },
  async getClassDetail(id) { await delay(150); return JSON.parse(JSON.stringify(MOCK.classes.find(c => c.id === id) || {})) },
  async createClass(body) { await delay(200); const c = { id: Date.now(), className: '新班级', companyId: body.companyId, week: body.week, startSession: body.startSession, endSession: body.endSession, status: 1, studentCount: 0 }; MOCK.classes.push(c); return c },
  async updateClass(id, body) { await delay(200); const c = MOCK.classes.find(c => c.id === id); if (c) { Object.assign(c, body) } return true },
  async deleteClass(id) { await delay(200); const i = MOCK.classes.findIndex(c => c.id === id); if (i >= 0) MOCK.classes.splice(i, 1); return true },

  // ========== 学生与助教总表 ==========
  async masterList(params) {
    await delay(150)
    const kw = (params && params.keyword || '').trim().toLowerCase()
    let list = MOCK.students
    if (kw) list = list.filter(s => (s.name && s.name.toLowerCase().includes(kw)) || (s.studentId && s.studentId.toLowerCase().includes(kw)))
    return paginate(list, params && params.page, params && params.size)
  },

  // ========== 学生 ==========
  async pageStudents(params) { await delay(150); return paginate(MOCK.students, params && params.page, params && params.size) },
  async getStudent(id) { await delay(150); return JSON.parse(JSON.stringify(MOCK.students.find(s => s.id === id) || {})) },
  async createStudent(body) { await delay(200); const s = { id: Date.now(), studentId: body.studentId, name: body.name, classId: body.classId, className: (MOCK.classes.find(c => c.id === body.classId) || {}).className || '', studentNoInClass: body.studentNoInClass || 1, originalMajor: body.originalMajor || '', gender: body.gender || 0, identity: 'STUDENT', isAssistant: false }; MOCK.students.push(s); return s },
  async updateStudent(id, body) { await delay(200); const s = MOCK.students.find(s => s.id === id); if (s) { Object.assign(s, body); if (body.classId) s.className = (MOCK.classes.find(c => c.id === body.classId) || {}).className || '' } return true },
  async deleteStudent(id) { await delay(200); const i = MOCK.students.findIndex(s => s.id === id); if (i >= 0) MOCK.students.splice(i, 1); return true },

  // ========== 管理员用户 ==========
  async pageUsers(params) {
    await delay(150)
    const kw = (params && params.keyword || '').trim().toLowerCase()
    let list = MOCK.users
    if (kw) list = list.filter(u => (u.username && u.username.toLowerCase().includes(kw)) || (u.realName && u.realName.toLowerCase().includes(kw)))
    return paginate(list, params && params.page, params && params.size)
  },
  async getUser(userId) { await delay(150); return JSON.parse(JSON.stringify(MOCK.users.find(u => u.id === userId) || {})) },
  async createTeacher(username, realName) { await delay(200); MOCK.users.push({ id: Date.now(), username, realName, roles: ['TEACHER'], status: 1, firstLogin: true, lastPasswordChangeTime: null, createdAt: new Date().toISOString() }); return true },
  async setUserStatus(userId, status) { await delay(200); const u = MOCK.users.find(u => u.id === userId); if (u) u.status = status; return true },
  async resetUserPassword(userId) { await delay(200); return true },
  async getUserClasses(userId) { await delay(150); return [1, 2] },
  async setUserClasses(userId, classIds) { await delay(200); return true },

  // ========== Excel 导入 ==========
  async downloadStudentTemplate() { await delay(200); return { data: mockArrayBuffer() } },
  async downloadAssistantTemplate() { await delay(200); return { data: mockArrayBuffer() } },
  async importStudents() { await delay(500); return { total: 5, successCount: 5, failCount: 0, errors: [] } },
  async importAssistants() { await delay(500); return { total: 2, successCount: 2, failCount: 0, errors: [] } },

  // ========== 学生导出 ==========
  async exportStudents() { await delay(200); return { data: mockArrayBuffer() } },
  async exportAssistants() { await delay(200); return { data: mockArrayBuffer() } },

  // ========== 助教管理 ==========
  async pageAssistants(page, size, keyword) { await delay(150); const list = MOCK.students.filter(s => s.isAssistant); return paginate(list, page, size) },
  async getAssistantClasses() { await delay(150); return [1] },
  async assignAssistantClasses() { await delay(200); return true },
  async revokeAssistant() { await delay(200); return true },
  async setAssistantIdentity() { await delay(200); return true },
  async listUnassignedAssistants() { await delay(150); return [] },

  // ========== 考勤与单次成绩 ==========
  async listAttendanceClasses() { await delay(150); return MOCK.classes.map(c => ({ id: c.id, className: c.className, classCode: c.classCode, studentCount: c.studentCount })) },
  async listAttendanceSessions(classId) { await delay(150); return MOCK.attendanceSessions.filter(s => s.classId === classId || !classId) },
  async createAttendanceSession(body) { await delay(200); const s = { id: Date.now(), classId: body.classId, weekNo: body.weekNo, sessionDate: body.sessionDate || new Date().toISOString().substring(0, 10), isLastSession: body.isLastSession || false, status: 1, sealed: false }; MOCK.attendanceSessions.push(s); return s },
  async updateAttendanceSession(sessionId, body) { await delay(200); const s = MOCK.attendanceSessions.find(s => s.id === sessionId); if (s) Object.assign(s, body); return true },
  async listAttendanceRecords(sessionId) { await delay(150); return JSON.parse(JSON.stringify(MOCK.attendanceRecords[sessionId] || [])) },
  async exportAttendanceRecords() { await delay(200); return { data: mockArrayBuffer() } },
  async saveAttendanceRecord(sessionId, studentId, body) {
    await delay(200)
    const records = MOCK.attendanceRecords[sessionId] || (MOCK.attendanceRecords[sessionId] = [])
    let rec = records.find(r => r.studentId === studentId)
    if (rec) { Object.assign(rec, body) } else {
      const stu = MOCK.students.find(s => s.id === studentId)
      rec = { id: Date.now(), sessionId, studentId, studentNo: stu ? stu.studentId : '', studentName: stu ? stu.name : '', studentNoInClass: stu ? stu.studentNoInClass : null, ...body }
      records.push(rec)
    }
    return true
  },
  async deleteAttendanceRecord(sessionId, studentId) {
    await delay(200)
    const records = MOCK.attendanceRecords[sessionId]
    if (records) { const i = records.findIndex(r => r.studentId === studentId); if (i >= 0) records.splice(i, 1) }
    return true
  },

  // ========== 换证考试成绩 ==========
  async listCertificateScores() { await delay(150); return JSON.parse(JSON.stringify(MOCK.certificateScores)) },
  async exportCertificateScores() { await delay(200); return { data: mockArrayBuffer() } },
  async saveCertificateScore(studentId, body) {
    await delay(200)
    let rec = MOCK.certificateScores.find(r => r.studentId === studentId)
    if (rec) { rec.finalScore = body.finalScore; rec.remark = body.remark || '' } else {
      const stu = MOCK.students.find(s => s.id === studentId)
      rec = { id: Date.now(), studentId, studentNo: stu ? stu.studentId : '', studentName: stu ? stu.name : '', studentNoInClass: stu ? stu.studentNoInClass : null, finalScore: body.finalScore, remark: body.remark || '' }
      MOCK.certificateScores.push(rec)
    }
    return true
  },
  async deleteCertificateScore(studentId) { await delay(200); const i = MOCK.certificateScores.findIndex(r => r.studentId === studentId); if (i >= 0) MOCK.certificateScores.splice(i, 1); return true },

  // ========== 操作日志 ==========
  async pageOperationLogs(params) { await delay(150); return paginate(MOCK.operationLogs, params && params.page, params && params.size) },

  // 兼容原始 request 暴露
  _request: function() { return Promise.resolve({ data: {} }) }
}
