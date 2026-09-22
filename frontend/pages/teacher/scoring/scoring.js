const api = require('../../../utils/api.js')

Page({
  data: {
    // 班级
    classList: [],
    classOptions: [],
    classIndex: 0,
    // 课次
    sessionList: [],
    sessionOptions: [],
    sessionIndex: 0,
    // 学生打分列表
    students: [],
    loading: false,
    // 批量保存状态
    saving: false
  },

  onShow() {
    if (!this.data.classList.length) this.loadClasses()
  },

  // 拉取考勤班级
  loadClasses() {
    wx.showLoading({ title: '加载中...', mask: true })
    api.listAttendanceClasses().then(classes => {
      const list = (classes || []).map(c => ({
        id: c.id,
        className: c.className || c.classCode || ('班级#' + c.id)
      }))
      this.setData({
        classList: list,
        classOptions: list.map(c => c.className),
        classIndex: 0
      })
      wx.hideLoading()
      if (list.length) this.loadSessions(list[0].id)
    }).catch(err => {
      wx.hideLoading()
      wx.showToast({ title: (err && err.message) || '加载班级失败', icon: 'none' })
    })
  },

  onClassChange(e) {
    const idx = e.detail.value
    this.setData({ classIndex: idx, sessionList: [], sessionOptions: [], students: [] })
    const cls = this.data.classList[idx]
    if (cls) this.loadSessions(cls.id)
  },

  // 拉取课次
  loadSessions(classId) {
    wx.showLoading({ title: '加载课次...', mask: true })
    api.listAttendanceSessions(classId).then(sessions => {
      // 只显示进行中的课次（status=1），已封存的不可打分
      const list = (sessions || []).filter(s => s.status !== 0).map(s => ({
        id: s.id,
        weekNo: s.weekNo,
        sessionDate: s.sessionDate,
        status: s.status,
        label: `第${s.weekNo}周 (${s.sessionDate || '未排日期'})`
      }))
      this.setData({
        sessionList: list,
        sessionOptions: list.map(s => s.label),
        sessionIndex: 0
      })
      wx.hideLoading()
      if (list.length) this.loadRecords(list[0].id)
      else this.setData({ students: [] })
    }).catch(err => {
      wx.hideLoading()
      wx.showToast({ title: (err && err.message) || '加载课次失败', icon: 'none' })
    })
  },

  onSessionChange(e) {
    const idx = e.detail.value
    this.setData({ sessionIndex: idx })
    const session = this.data.sessionList[idx]
    if (session) this.loadRecords(session.id)
  },

  // 拉取全班考勤记录
  loadRecords(sessionId) {
    this.setData({ loading: true })
    wx.showLoading({ title: '加载学生...', mask: true })
    api.listAttendanceRecords(sessionId).then(records => {
      const students = (records || []).map(r => {
        let displayType = '正常'
        if (r.attendanceType === 'J') displayType = '事假'
        else if (r.attendanceType === 'K') displayType = '旷课'
        return {
          id: r.studentId,
          recordId: r.id,
          name: r.studentName || '',
          studentNo: r.studentNo || '',
          studentNoInClass: r.studentNoInClass,
          attendanceType: r.attendanceType || 'NORMAL',
          displayType,
          score: r.score !== null && r.score !== undefined ? String(r.score) : '',
          remark: r.remark || ''
        }
      })
      this.setData({ students, loading: false })
      wx.hideLoading()
    }).catch(err => {
      wx.hideLoading()
      this.setData({ loading: false })
      wx.showToast({ title: (err && err.message) || '加载失败', icon: 'none' })
    })
  },

  // 分数输入
  onScoreInput(e) {
    const idx = e.currentTarget.dataset.idx
    let val = e.detail.value
    // 只允许数字和小数点，范围 0-10
    val = val.replace(/[^\d.]/g, '')
    const num = parseFloat(val)
    if (!isNaN(num) && num > 10) val = '10'
    this.setData({ [`students[${idx}].score`]: val })
  },

  // 考勤类型切换
  onTypeChange(e) {
    const idx = e.currentTarget.dataset.idx
    const typeIdx = e.detail.value
    const types = ['NORMAL', 'J', 'K']
    const labels = ['正常', '事假', '旷课']
    const attendanceType = types[typeIdx]
    this.setData({
      [`students[${idx}].attendanceType`]: attendanceType,
      [`students[${idx}].displayType`]: labels[typeIdx]
    })
    // 事假/旷课时分数自动设为 0
    if (attendanceType !== 'NORMAL') {
      this.setData({ [`students[${idx}].score`]: '0' })
    }
  },

  // 备注输入
  onRemarkInput(e) {
    const idx = e.currentTarget.dataset.idx
    this.setData({ [`students[${idx}].remark`]: e.detail.value })
  },

  // 保存单个学生成绩
  onSaveOne(e) {
    const idx = e.currentTarget.dataset.idx
    const student = this.data.students[idx]
    if (!student) return
    this.saveStudent(student, idx)
  },

  // 批量保存
  onSaveAll() {
    if (this.data.saving) return
    const students = this.data.students
    if (!students.length) {
      wx.showToast({ title: '暂无学生数据', icon: 'none' })
      return
    }
    this.setData({ saving: true })
    wx.showLoading({ title: '批量保存中...', mask: true })

    const sessionId = this.data.sessionList[this.data.sessionIndex].id
    const promises = students.map(student => {
      const score = parseFloat(student.score)
      const body = {
        attendanceType: student.attendanceType,
        score: isNaN(score) ? 0 : score,
        remark: student.remark || ''
      }
      return api.saveAttendanceRecord(sessionId, student.id, body)
    })

    Promise.all(promises).then(() => {
      wx.hideLoading()
      this.setData({ saving: false })
      wx.showToast({ title: `已保存 ${students.length} 条`, icon: 'success' })
    }).catch(err => {
      wx.hideLoading()
      this.setData({ saving: false })
      wx.showToast({ title: (err && err.message) || '部分保存失败', icon: 'none' })
    })
  },

  // 保存单个
  saveStudent(student, idx) {
    const score = parseFloat(student.score)
    if (student.attendanceType === 'NORMAL' && (isNaN(score) || score < 0 || score > 10)) {
      wx.showToast({ title: '分数须在 0~10 之间', icon: 'none' })
      return
    }
    const sessionId = this.data.sessionList[this.data.sessionIndex].id
    wx.showLoading({ title: '保存中...', mask: true })
    api.saveAttendanceRecord(sessionId, student.id, {
      attendanceType: student.attendanceType,
      score: isNaN(score) ? 0 : score,
      remark: student.remark || ''
    }).then(() => {
      wx.hideLoading()
      wx.showToast({ title: '已保存', icon: 'success' })
    }).catch(err => {
      wx.hideLoading()
      wx.showToast({ title: (err && err.message) || '保存失败', icon: 'none' })
    })
  },

  onBack() {
    const pages = getCurrentPages()
    if (pages.length > 1) wx.navigateBack()
    else wx.reLaunch({ url: '/pages/teacher/teacher' })
  }
})
