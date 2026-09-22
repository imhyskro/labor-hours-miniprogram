const api = require('../../utils/api.js')

Page({
  data: {
    classId: null,
    className: '',
    // 课次
    sessionList: [],      // AttendanceSessionVO[]
    sessionOptions: [],   // 课次下拉显示文本
    sessionIndex: 0,
    currentSessionId: null,
    // 学生考勤记录
    students: [],
    // 键盘
    keyboardVisible: false,
    activeIdx: -1,
    bufferScore: '',
    keys: ['1', '2', '3', '4', '5', '6', '7', '8', '9', 'J', 'K', 'del', 'confirm'],
    loading: false
  },

  onLoad(options) {
    const classId = options.classId ? Number(options.classId) : null
    const className = decodeURIComponent(options.className || '班级')
    this.setData({ classId, className })

    if (!classId) {
      wx.showToast({ title: '缺少班级参数', icon: 'none' })
      return
    }
    this.loadSessions()
  },

  // 拉取班级课次
  loadSessions() {
    wx.showLoading({ title: '加载课次...', mask: true })
    api.listAttendanceSessions(this.data.classId).then(sessions => {
      const sessionList = (sessions || []).map(s => ({
        id: s.id,
        classId: s.classId,
        className: s.className || '',
        weekNo: s.weekNo,
        sessionDate: s.sessionDate || '',
        isLastSession: s.isLastSession,
        status: s.status,
        statusText: s.status === 1 ? '可编辑' : '已封存'
      }))
      const sessionOptions = sessionList.map(s => `第${s.weekNo}周${s.sessionDate ? '·' + s.sessionDate : ''}${s.isLastSession === 1 ? '·末次' : ''}`)
      const idx = Math.min(0, sessionList.length - 1)
      this.setData({ sessionList, sessionOptions, sessionIndex: Math.max(0, idx) })
      wx.hideLoading()
      if (sessionList.length) {
        this.setData({ currentSessionId: sessionList[Math.max(0, idx)].id })
        this.loadRecords()
      } else {
        this.setData({ students: [] })
        wx.showToast({ title: '该班级暂无课次', icon: 'none' })
      }
    }).catch(err => {
      wx.hideLoading()
      wx.showToast({ title: (err && err.message) || '加载课次失败', icon: 'none' })
    })
  },

  onSessionChange(e) {
    const idx = e.detail.value
    const session = this.data.sessionList[idx]
    this.setData({ sessionIndex: idx, currentSessionId: session ? session.id : null })
    if (session) this.loadRecords()
  },

  // 拉取全班考勤与单次分数
  loadRecords() {
    const sessionId = this.data.currentSessionId
    if (!sessionId) return
    wx.showLoading({ title: '加载考勤...', mask: true })
    api.listAttendanceRecords(sessionId).then(records => {
      // AttendanceRecordVO: id, sessionId, studentId, studentNo, studentName,
      // studentNoInClass, attendanceType, score, remark, recordedBy, updatedAt
      const students = (records || []).map(r => ({
        id: r.studentId,
        recordId: r.id,
        studentNo: r.studentNo || '',
        name: r.studentName || '',
        studentNoInClass: r.studentNoInClass,
        attendanceType: r.attendanceType || '',
        score: r.score !== null && r.score !== undefined ? Number(r.score) : null,
        remark: r.remark || '',
        // 用于显示：已登记的分数或考勤状态
        inputScore: this.formatDisplay(r),
        highlight: false,
        // 保留 btnState 兼容旧 UI（修改按钮）
        btnState: 'normal',
        needApproval: false,
        modifyTimestamp: null
      }))
      this.setData({ students })
      wx.hideLoading()
    }).catch(err => {
      wx.hideLoading()
      wx.showToast({ title: (err && err.message) || '加载考勤失败', icon: 'none' })
    })
  },

  // 格式化考勤显示文本
  formatDisplay(r) {
    if (!r.attendanceType) return ''
    if (r.attendanceType === 'NORMAL') {
      return r.score !== null && r.score !== undefined ? String(r.score) : ''
    }
    if (r.attendanceType === 'J') return '事假'
    if (r.attendanceType === 'K') return '旷课'
    return ''
  },

  // 新建课次（教师/超管可用，助教无权限）
  onCreateSession() {
    const { classId, sessionList } = this.data
    if (!classId) return
    // 默认周次为已有最大周次 + 1
    const nextWeek = sessionList.length ? Math.max(...sessionList.map(s => s.weekNo || 0)) + 1 : 1
    const today = new Date()
    const pad = n => (n < 10 ? '0' + n : '' + n)
    const sessionDate = `${today.getFullYear()}-${pad(today.getMonth() + 1)}-${pad(today.getDate())}`

    wx.showModal({
      title: '新建课次',
      editable: true,
      placeholderText: `周次（默认 ${nextWeek}）`,
      content: String(nextWeek),
      success: res => {
        if (!res.confirm) return
        const weekNo = parseInt((res.content || '').trim(), 10) || nextWeek
        wx.showLoading({ title: '创建中...', mask: true })
        api.createAttendanceSession({ classId, weekNo, sessionDate, isLastSession: 0 }).then(data => {
          wx.hideLoading()
          wx.showToast({ title: '课次已创建', icon: 'success' })
          this.loadSessions()
        }).catch(err => {
          wx.hideLoading()
          wx.showToast({ title: (err && err.message) || '创建失败', icon: 'none' })
        })
      }
    })
  },

  // 导出课次考勤 Excel
  onExportRecords() {
    const sessionId = this.data.currentSessionId
    if (!sessionId) {
      wx.showToast({ title: '请先选择课次', icon: 'none' })
      return
    }
    wx.showLoading({ title: '导出中...', mask: true })
    api.exportAttendanceRecords(sessionId).then(res => {
      wx.hideLoading()
      this.saveAndOpenExcel(res.data, `考勤数据-课次${sessionId}.xlsx`)
    }).catch(err => {
      wx.hideLoading()
      wx.showToast({ title: (err && err.message) || '导出失败', icon: 'none' })
    })
  },

  // 封存课次（教师/超管可用，助教无权限）
  onSealSession() {
    const session = this.data.sessionList[this.data.sessionIndex]
    if (!session) return
    if (session.status === 0) {
      wx.showToast({ title: '该课次已封存', icon: 'none' })
      return
    }
    wx.showModal({
      title: '封存课次',
      content: `封存「第${session.weekNo}周」课次后，考勤记录将不能再修改。确认封存？`,
      success: r => {
        if (!r.confirm) return
        wx.showLoading({ title: '封存中...', mask: true })
        api.updateAttendanceSession(session.id, {
          weekNo: session.weekNo,
          sessionDate: session.sessionDate,
          isLastSession: session.isLastSession,
          status: 0
        }).then(() => {
          wx.hideLoading()
          wx.showToast({ title: '已封存', icon: 'success' })
          this.loadSessions()
        }).catch(err => {
          wx.hideLoading()
          wx.showToast({ title: (err && err.message) || '封存失败', icon: 'none' })
        })
      }
    })
  },

  // 删除单个学生考勤记录
  onDeleteRecord(e) {
    const idx = e.currentTarget.dataset.idx
    const student = this.data.students[idx]
    if (!student) return
    if (!student.recordId) {
      wx.showToast({ title: '该学生暂无考勤记录', icon: 'none' })
      return
    }
    const session = this.data.sessionList[this.data.sessionIndex]
    if (session && session.status === 0) {
      wx.showToast({ title: '课次已封存，不可修改', icon: 'none' })
      return
    }
    wx.showModal({
      title: '删除考勤',
      content: `删除「${student.name}」本课次考勤记录？`,
      success: r => {
        if (!r.confirm) return
        wx.showLoading({ title: '删除中...', mask: true })
        api.deleteAttendanceRecord(this.data.currentSessionId, student.id).then(() => {
          wx.hideLoading()
          wx.showToast({ title: '已删除', icon: 'success' })
          this.loadRecords()
        }).catch(err => {
          wx.hideLoading()
          wx.showToast({ title: (err && err.message) || '删除失败', icon: 'none' })
        })
      }
    })
  },

  saveAndOpenExcel(buf, fileName) {
    if (!buf) {
      wx.showToast({ title: '文件为空', icon: 'none' })
      return
    }
    const fs = wx.getFileSystemManager()
    const filePath = `${wx.env.USER_DATA_PATH}/${fileName}`
    fs.writeFile({
      filePath,
      data: buf,
      encoding: 'binary',
      success: () => {
        wx.showModal({
          title: '导出成功',
          content: '是否打开文件？',
          success: r => {
            if (r.confirm) {
              wx.openDocument({ filePath, showMenu: true, fail: () => wx.showToast({ title: '请在聊天中查看文件', icon: 'none' }) })
            }
          }
        })
      },
      fail: () => wx.showToast({ title: '文件保存失败', icon: 'none' })
    })
  },

  // 打分框点击：弹键盘
  onScoreTap(e) {
    const idx = e.currentTarget.dataset.idx
    const student = this.data.students[idx]
    // 已封存课次不允许修改
    const session = this.data.sessionList[this.data.sessionIndex]
    if (session && session.status === 0) {
      wx.showToast({ title: '课次已封存，不可修改', icon: 'none' })
      return
    }
    this.setData({
      keyboardVisible: true,
      activeIdx: idx,
      bufferScore: ''
    })
  },

  // 键盘按键
  onKeyTap(e) {
    const key = e.currentTarget.dataset.key
    const idx = this.data.activeIdx
    if (idx < 0) return

    if (key === 'del') {
      this.setData({ bufferScore: '' })
    } else if (key === 'confirm') {
      this.confirmScore()
    } else {
      // 1-9 或 J 或 K
      this.setData({ bufferScore: key })
    }
  },

  // 确认提交考勤
  confirmScore() {
    const { bufferScore, activeIdx, currentSessionId, students } = this.data
    if (activeIdx < 0 || !currentSessionId) return

    const student = students[activeIdx]
    if (!student) return

    if (!bufferScore) {
      wx.showToast({ title: '请先输入分数或考勤状态', icon: 'none' })
      return
    }

    // 构造请求体
    let attendanceType, score
    if (/^\d$/.test(bufferScore)) {
      // 数字 → NORMAL + score
      attendanceType = 'NORMAL'
      score = Number(bufferScore)
    } else if (bufferScore === 'J') {
      attendanceType = 'J'
      score = 0
    } else if (bufferScore === 'K') {
      attendanceType = 'K'
      score = 0
    } else {
      wx.showToast({ title: '输入无效', icon: 'none' })
      return
    }

    wx.showLoading({ title: '提交中...', mask: true })
    api.saveAttendanceRecord(currentSessionId, student.id, {
      attendanceType,
      score,
      remark: ''
    }).then(() => {
      wx.hideLoading()
      // 更新本地显示
      const displayText = attendanceType === 'NORMAL' ? String(score)
        : (attendanceType === 'J' ? '事假' : '旷课')
      const updates = {
        [`students[${activeIdx}].inputScore`]: displayText,
        [`students[${activeIdx}].attendanceType`]: attendanceType,
        [`students[${activeIdx}].score`]: score,
        keyboardVisible: false,
        activeIdx: -1,
        bufferScore: ''
      }
      this.setData(updates)
      wx.showToast({ title: '已保存', icon: 'success' })
    }).catch(err => {
      wx.hideLoading()
      wx.showToast({ title: (err && err.message) || '保存失败', icon: 'none' })
    })
  },

  // 遮罩收起
  onHideKeyboard() {
    this.setData({ keyboardVisible: false, activeIdx: -1, bufferScore: '' })
  },

  // 修改按钮（保留旧流程，跳修改申请页）
  onGoModify(e) {
    const idx = e.currentTarget.dataset.idx
    const student = this.data.students[idx]
    const { className } = this.data
    wx.navigateTo({
      url: `/pages/modifyEdit/modifyEdit?studentId=${student.id}&studentName=${encodeURIComponent(student.name)}&originScore=${student.inputScore || ''}&className=${encodeURIComponent(className)}`
    })
  }
})
