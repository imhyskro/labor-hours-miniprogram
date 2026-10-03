const app = getApp()
const api = require('../../utils/api.js')
const auth = require('../../utils/auth.js')

Page({
  data: {
    classId: null,
    className: '',
    // 课次
    sessionList: [],      // AttendanceSessionVO[]（含 unscoredCount）
    sessionOptions: [],   // 课次下拉显示文本
    sessionIndex: 0,
    currentSessionId: null,
    currentSession: {},    // 当前选中课次对象（用于显示区红点）
    sessionPickerVisible: false, // 课次滑动选择器弹层显隐
    tempSessionIndex: 0,        // 弹层中临时选中的下标
    // 学生考勤记录
    students: [],         // 后端原始记录
    filteredStudents: [],// 搜索后的展示列表
    searchKeyword: '',
    // 键盘
    keyboardVisible: false,
    activeIdx: -1,
    bufferScore: '',
    keys: ['1', '2', '3', '4', '5', '6', '7', '8', '9', 'J', 'K', 'del', 'confirm'],
    loading: false,
    saving: false     // 保存中状态
  },

  onLoad(options) {
    if (!auth.isAssistantOnly()) {
      auth.clearAuth()
      wx.reLaunch({ url: '/pages/login/login' })
      return
    }
    const classId = options.classId ? Number(options.classId) : null
    const className = decodeURIComponent(options.className || '班级')
    this._requestedSessionId = options.sessionId ? Number(options.sessionId) : null
    this._focusStudentId = options.focusStudentId ? Number(options.focusStudentId) : null
    this._autoOpenApproved = options.autoOpen === '1'
    this.setData({ classId, className })

    if (!classId) {
      wx.showToast({ title: '缺少班级参数', icon: 'none' })
      return
    }
    this.loadSessions()
    this._firstShow = true
  },

  // 返回本页时刷新审批状态（modifyEdit 提交后状态回写）
  onShow() {
    if (this._firstShow) { this._firstShow = false; return }
    if (this.data.classId) this.loadSessions()
  },

  // 拉取班级课次
  loadSessions() {
    wx.showLoading({ title: '加载课次...', mask: true })
    api.listAttendanceSessions(this.data.classId).then(sessions => {
      const sessionList = (sessions || []).map(s => {
        const window = this.getSessionWindow(s)
        const editable = typeof s.editable === 'boolean' ? s.editable : window.editable
        const lockReason = s.lockReason || window.reason
        return {
          id: s.id,
          classId: s.classId,
          className: s.className || '',
          weekNo: s.weekNo,
          sessionDate: s.sessionDate || '',
          isLastSession: s.isLastSession,
          status: s.status,
          sealDays: s.sealDays || 10,
          sealDate: s.sealDate || window.sealDate,
          editable,
          lockReason,
          unscoredCount: editable ? (s.unscoredCount || 0) : 0,
          statusText: editable ? '可编辑' : lockReason
        }
      })
      const sessionOptions = sessionList.map(s => `周${s.weekNo}${s.sessionDate ? '·' + s.sessionDate : ''}${s.isLastSession === 1 ? '·末次' : ''}`)
      const requestedIdx = this._requestedSessionId
        ? sessionList.findIndex(item => Number(item.id) === this._requestedSessionId)
        : -1
      const curIdx = requestedIdx >= 0 ? requestedIdx : this.findCurrentSessionIndex(sessionList)
      this.setData({
        sessionList, sessionOptions, sessionIndex: curIdx,
        currentSession: sessionList[curIdx] || {}
      })
      wx.hideLoading()
      if (sessionList.length) {
        this.setData({ currentSessionId: sessionList[curIdx].id })
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

  // 小程序按当前日期复核：课次当天开放，达到配置天数时自动封存。
  getSessionWindow(session) {
    if (!session) return { editable: false, reason: '暂无周次', sealDate: '' }
    if (!session.sessionDate) {
      return { editable: false, reason: '课次日期未设置', sealDate: '' }
    }
    const parts = String(session.sessionDate).split('-').map(Number)
    if (parts.length !== 3 || parts.some(n => !Number.isFinite(n))) {
      return { editable: false, reason: '课次日期无效', sealDate: '' }
    }
    const start = new Date(parts[0], parts[1] - 1, parts[2])
    const now = new Date()
    const today = new Date(now.getFullYear(), now.getMonth(), now.getDate())
    const sealDays = Math.max(1, Number(session.sealDays) || 10)
    const sealAt = new Date(start.getFullYear(), start.getMonth(), start.getDate() + sealDays)
    const pad = n => (n < 10 ? '0' + n : String(n))
    const sealDate = `${sealAt.getFullYear()}-${pad(sealAt.getMonth() + 1)}-${pad(sealAt.getDate())}`
    if (today < start) return { editable: false, reason: '未到本周打分时间', sealDate }
    if (today >= sealAt) return { editable: false, reason: `已超过${sealDays}天打分期限`, sealDate }
    return { editable: true, reason: '可编辑', sealDate }
  },

  findCurrentSessionIndex(sessionList) {
    if (!sessionList.length) return 0
    for (let i = sessionList.length - 1; i >= 0; i--) {
      if (sessionList[i].editable) return i
    }
    const now = new Date()
    const todayText = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`
    for (let i = sessionList.length - 1; i >= 0; i--) {
      if (sessionList[i].sessionDate && sessionList[i].sessionDate <= todayText) return i
    }
    return 0
  },

  ensureSessionEditable() {
    const session = this.data.currentSession
    if (session && session.editable) return true
    wx.showToast({ title: (session && session.lockReason) || '当前周次不可打分', icon: 'none' })
    return false
  },

  // 点击课次选择框 → 弹出滑动选择器
  onSessionPickerTap() {
    this.setData({ sessionPickerVisible: true, tempSessionIndex: this.data.sessionIndex })
  },

  // 弹层中点某项
  onSessionOptionTap(e) {
    this.setData({ tempSessionIndex: e.currentTarget.dataset.index })
  },

  // 确定切换课次
  onSessionPickerConfirm() {
    const idx = this.data.tempSessionIndex
    const session = this.data.sessionList[idx]
    this.setData({
      sessionIndex: idx,
      currentSession: session || {},
      currentSessionId: session ? session.id : null,
      sessionPickerVisible: false
    })
    if (!session) return
    if (!session.editable) {
      wx.showToast({ title: session.lockReason || '当前周次仅供查看', icon: 'none' })
    }
    this.loadRecords()
  },

  // 取消
  onSessionPickerCancel() {
    this.setData({ sessionPickerVisible: false })
  },

  // 拉取全班考勤与单次分数
  loadRecords() {
    const sessionId = this.data.currentSessionId
    if (!sessionId) return Promise.resolve()
    wx.showLoading({ title: '加载考勤...', mask: true })
    return api.listAttendanceRecords(sessionId).then(records => {
      // AttendanceRecordVO: id, sessionId, studentId, studentNo, studentName,
      // studentNoInClass, attendanceType, score, remark, recordedBy, updatedAt
      const students = (records || []).map(r => ({
        id: r.studentId,
        recordId: r.id,
        studentNo: r.studentNo || '',
        name: r.studentName || '',
        studentNoInClass: r.studentNoInClass,
        grade: r.grade || '',
        college: r.college || '',
        major: r.major || '',
        isAssistant: r.isAssistant === 1,
        attendanceType: r.attendanceType || '',
        score: r.score !== null && r.score !== undefined ? Number(r.score) : null,
        approvalStatus: r.approvalStatus || '',
        changeRequestId: r.availableChangeRequestId || null,
        remark: r.remark || '',
        inputScore: this.formatDisplay(r),
        highlight: false,
        btnState: 'normal',
        needApproval: r.approvalStatus === 'pending',
        modifyTimestamp: null
      }))
      // 先写回 students，修复搜索 Bug：之前未写回导致 this.data.students 恒为空
      this.setData({ students }, () => {
        this.applySearch(students)
        if (this._autoOpenApproved && this._focusStudentId) {
          const index = students.findIndex(item => Number(item.id) === this._focusStudentId)
          if (index >= 0 && students[index].approvalStatus === 'approved') {
            this.setData({
              keyboardVisible: true,
              activeIdx: index,
              bufferScore: ''
            })
          }
          this._autoOpenApproved = false
        }
      })
      wx.hideLoading()
    }).catch(err => {
      wx.hideLoading()
      wx.showToast({ title: (err && err.message) || '加载考勤失败', icon: 'none' })
    })
  },

  // 格式化考勤显示文本
  formatDisplay(r) {
    if (r.isAssistant === 1) return '助教'
    if (!r.attendanceType) return ''
    if (r.attendanceType === 'NORMAL') {
      return r.score !== null && r.score !== undefined ? String(r.score) : ''
    }
    if (r.attendanceType === 'J') return '事假'
    if (r.attendanceType === 'K') return '旷课'
    return ''
  },

  // 一键打分：仅为未打分的普通学生赋基础分，助教身份自动跳过。
  onBatchScore() {
    const { currentSessionId, filteredStudents } = this.data
    if (!currentSessionId) {
      wx.showToast({ title: '请先选择课次', icon: 'none' })
      return
    }
    if (!this.ensureSessionEditable()) return
    const targets = filteredStudents.filter(s =>
      !s.isAssistant &&
      (s.score === null || s.score === undefined || s.score === '') &&
      !s.attendanceType
    )
    if (!targets.length) {
      wx.showToast({ title: '当前无待打分学生', icon: 'none' })
      return
    }
    const ids = targets.map(s => s.id)
    wx.showModal({
      title: '一键打分',
      content: `将为 ${ids.length} 名未打分同学统一设为 8 分（已有分数的同学将跳过）`,
      confirmColor: '#4A6B3A',
      success: r => {
        if (!r.confirm) return
        wx.showLoading({ title: '打分中...', mask: true })
        api.batchScore(currentSessionId, ids, 8).then(res => {
          wx.hideLoading()
          const n = (res && res.success != null) ? res.success : ids.length
          wx.showToast({ title: `已为 ${n} 名同学打分`, icon: 'success' })
          app.globalData.classListDirty = true
          // 一键打分已经直接持久化，刷新周次和学生数据即可。
          this.loadSessions()
        }).catch(err => {
          wx.hideLoading()
          wx.showToast({ title: (err && err.message) || '打分失败', icon: 'none' })
        })
      }
    })
  },

  // 保存按钮：检查是否全部打完分，是则清除该班级待办状态
  onSaveAll(fromBatch) {
    if (this.data.saving) return
    const { filteredStudents, currentSessionId } = this.data
    if (!currentSessionId) {
      wx.showToast({ title: '请先选择课次', icon: 'none' })
      return
    }
    if (!this.ensureSessionEditable()) return
    // 检查是否还有未打分学生
    const unscored = filteredStudents.filter(s =>
      !s.isAssistant && (s.score === null || s.score === undefined || s.score === '') && !s.attendanceType
    )
    if (unscored.length > 0) {
      wx.showModal({
        title: '还有未打分学生',
        content: `当前还有 ${unscored.length} 名同学未打分，完成全部打分后才能消除待办。是否继续？`,
        confirmText: '继续打分',
        showCancel: false,
        confirmColor: '#4A6B3A'
      })
      return
    }
    // 全部打完分 → 标记全局 dirty + 重新拉取课次列表刷新本页红点
    this.setData({ saving: true })
    setTimeout(() => {
      app.globalData.classListDirty = true
      this.setData({ saving: false })
      wx.showToast({
        title: fromBatch ? '已保存，待办已消除' : '保存成功，待办已消除',
        icon: 'success'
      })
      // 重新拉取课次，刷新 unscoredCount（当前页红点立即消失）
      this.loadSessions()
    }, 400)
  },

  // 搜索输入
  onSearchInput(e) {
    const keyword = (e.detail.value || '').trim()
    this.setData({ searchKeyword: keyword }, () => this.applySearch(this.data.students))
  },

  // 搜索确认：定位高亮到第一个命中项
  onSearchConfirm() {
    const { filteredStudents } = this.data
    if (!filteredStudents.length) {
      wx.showToast({ title: '未找到该学生', icon: 'none' })
      return
    }
    const first = filteredStudents[0]
    const query = wx.createSelectorQuery().in(this)
    query.select('#student-' + first.id).boundingClientRect(rect => {
      if (rect && rect.top) {
        wx.pageScrollTo({ scrollTop: rect.top + (this._lastScrollTop || 0), duration: 300 })
      }
      this.setData({ ['filteredStudents[0].highlight']: true })
      setTimeout(() => this.setData({ ['filteredStudents[0].highlight']: false }), 2000)
    }).exec()
  },

  // 退出搜索：清空关键字、恢复完整列表
  onExitSearch() {
    this.setData({ searchKeyword: '' }, () => this.applySearch(this.data.students))
  },

  // 应用搜索过滤 + 置顶排序（需审批/待修改优先）
  applySearch(allStudents) {
    const kw = (this.data.searchKeyword || '').toLowerCase()
    let filtered = allStudents
    if (kw) {
      // String() 兜底字段可能为数字类型，避免 toLowerCase 报错
      filtered = allStudents.filter(s =>
        String(s.studentNo || '').toLowerCase().indexOf(kw) >= 0 ||
        String(s.name || '').toLowerCase().indexOf(kw) >= 0
      )
    }
    // 置顶排序：approvalStatus 非 empty 的排在前面
    const TOP_ORDER = { pending: 0, approved: 0 }
    filtered = filtered.slice().sort((a, b) => {
      const av = (a.approvalStatus && TOP_ORDER[a.approvalStatus] !== undefined) ? 0 : 1
      const bv = (b.approvalStatus && TOP_ORDER[b.approvalStatus] !== undefined) ? 0 : 1
      return av - bv
    })
    this.setData({ filteredStudents: filtered })
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
    const student = this.data.filteredStudents[idx]
    if (!student) return
    if (student.isAssistant) {
      wx.showToast({ title: '助教身份学生不参与打分', icon: 'none' })
      return
    }
    if (!this.data.currentSession.editable && student.approvalStatus !== 'approved') {
      this.ensureSessionEditable()
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
    const { bufferScore, activeIdx, currentSessionId, filteredStudents } = this.data
    if (activeIdx < 0 || !currentSessionId) return

    const student = filteredStudents[activeIdx]
    if (!student) return
    if (student.isAssistant) return
    if (!this.data.currentSession.editable && student.approvalStatus !== 'approved') {
      this.ensureSessionEditable()
      return
    }

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
      remark: '',
      changeRequestId: student.changeRequestId || null
    }).then(() => {
      wx.hideLoading()
      const displayText = attendanceType === 'NORMAL' ? String(score)
        : (attendanceType === 'J' ? '事假' : '旷课')
      // 同步更新原始列表与过滤列表
      const sid = student.id
      const allIdx = this.data.students.findIndex(s => s.id === sid)
      const updates = {}
      if (allIdx >= 0) {
        updates[`students[${allIdx}].inputScore`] = displayText
        updates[`students[${allIdx}].attendanceType`] = attendanceType
        updates[`students[${allIdx}].score`] = score
        updates[`students[${allIdx}].approvalStatus`] = ''
        updates[`students[${allIdx}].changeRequestId`] = null
      }
      updates[`filteredStudents[${activeIdx}].inputScore`] = displayText
      updates[`filteredStudents[${activeIdx}].attendanceType`] = attendanceType
      updates[`filteredStudents[${activeIdx}].score`] = score
      updates[`filteredStudents[${activeIdx}].approvalStatus`] = ''
      updates[`filteredStudents[${activeIdx}].changeRequestId`] = null
      updates.keyboardVisible = false
      updates.activeIdx = -1
      updates.bufferScore = ''
      this.setData(updates)
      // 标记班级列表需刷新红点：返回班级页 onShow 时重新检查
      app.globalData.classListDirty = true
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

  // 修改按钮（跳修改申请页，携带姓名/学号/学院/专业/课次ID，供回写审批状态）
  onGoModify(e) {
    const idx = e.currentTarget.dataset.idx
    const student = this.data.filteredStudents[idx]
    if (!student) return
    if (student.isAssistant) {
      wx.showToast({ title: '助教身份学生不参与打分', icon: 'none' })
      return
    }
    const { className, currentSessionId } = this.data
    wx.navigateTo({
      url: `/pages/modifyEdit/modifyEdit?studentId=${student.id}&studentName=${encodeURIComponent(student.name)}&studentNo=${encodeURIComponent(student.studentNo || '')}&college=${encodeURIComponent(student.college || '')}&major=${encodeURIComponent(student.major || '')}&grade=${encodeURIComponent(student.grade || '')}&originScore=${student.inputScore || ''}&className=${encodeURIComponent(className)}&sessionId=${currentSessionId || ''}`
    })
  }
})
