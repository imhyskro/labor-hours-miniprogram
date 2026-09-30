const api = require('../../utils/api.js')

const STATUS_TEXT = {
  PENDING: '待审核',
  APPROVED: '待修改',
  REJECTED: '已驳回',
  USED: '已完成修改',
  CANCELLED: '已撤销',
  EXPIRED: '已失效'
}

Page({
  data: {
    classId: null,
    className: '',
    studentFilter: null,
    studentName: '',
    list: [],
    loading: false,
    countdownTimer: null
  },

  onLoad(options) {
    this.setData({
      classId: options.classId ? Number(options.classId) : null,
      className: decodeURIComponent(options.className || ''),
      studentFilter: options.studentId ? Number(options.studentId) : null,
      studentName: decodeURIComponent(options.studentName || '')
    })
  },

  onShow() {
    this.loadData()
    this.startCountdown()
  },

  onUnload() {
    this.clearCountdown()
  },

  loadData() {
    this.setData({ loading: true })
    api.listModificationRequests().then(rows => {
      const classId = this.data.classId
      const studentFilter = this.data.studentFilter
      const list = (rows || [])
        .filter(item => !classId || Number(item.teachingGroupId) === Number(classId))
        .filter(item => !studentFilter || Number(item.studentId) === Number(studentFilter))
        .map(item => this.mapRequest(item))
      this.setData({ list, loading: false })
    }).catch(err => {
      this.setData({ loading: false })
      wx.showToast({ title: (err && err.message) || '申请记录加载失败', icon: 'none' })
    })
  },

  mapRequest(item) {
    const currentScore = item.currentScoreMark || this.formatScore(item.currentScore)
    return this.computeCountdown({
      id: item.id,
      requestNo: item.requestNo,
      studentId: item.studentId,
      studentName: item.studentName,
      studentNo: item.studentNo,
      teachingGroupId: item.teachingGroupId,
      sessionId: item.sessionId,
      sessionDate: item.sessionDate,
      originScore: currentScore,
      currentScore,
      applyReason: item.reason,
      statusCode: item.status,
      status: item.status === 'USED' ? 'modified' : String(item.status || '').toLowerCase(),
      statusText: STATUS_TEXT[item.status] || item.status,
      applyTime: this.formatDateTime(item.createdAt),
      editWindowExpiresAt: item.editWindowExpiresAt,
      teacher: '—',
      revokeCountdown: ''
    })
  },

  formatScore(value) {
    return value === null || value === undefined ? '—' : String(value)
  },

  formatDateTime(value) {
    return value ? String(value).replace('T', ' ').slice(0, 16) : '—'
  },

  computeCountdown(item) {
    item.revokeCountdown = ''
    if (item.statusCode === 'APPROVED' && item.editWindowExpiresAt) {
      const expiresAt = new Date(String(item.editWindowExpiresAt).replace(' ', 'T')).getTime()
      const remainMs = expiresAt - Date.now()
      if (remainMs <= 0) {
        item.status = 'expired'
        item.statusText = STATUS_TEXT.EXPIRED
        item.revokeCountdown = '修改窗口已过期'
      } else {
        const hours = Math.ceil(remainMs / (60 * 60 * 1000))
        item.revokeCountdown = hours >= 24
          ? `${Math.ceil(hours / 24)}天内完成修改`
          : `${hours}小时内完成修改`
      }
    }
    return item
  },

  startCountdown() {
    this.clearCountdown()
    this.data.countdownTimer = setInterval(() => {
      this.setData({ list: this.data.list.map(item => this.computeCountdown({ ...item })) })
    }, 60 * 1000)
  },

  clearCountdown() {
    if (this.data.countdownTimer) {
      clearInterval(this.data.countdownTimer)
      this.data.countdownTimer = null
    }
  },

  onRevoke(e) {
    const id = e.currentTarget.dataset.id
    wx.showModal({
      title: '确认撤销',
      content: '撤销后该申请将作废，是否继续？',
      confirmColor: '#4A6B3A',
      success: res => {
        if (!res.confirm) return
        wx.showLoading({ title: '撤销中...', mask: true })
        api.cancelModificationRequest(id).then(() => {
          wx.hideLoading()
          wx.showToast({ title: '已撤销申请', icon: 'success' })
          this.loadData()
        }).catch(err => {
          wx.hideLoading()
          wx.showToast({ title: (err && err.message) || '撤销失败', icon: 'none' })
        })
      }
    })
  },

  onModify(e) {
    const item = e.currentTarget.dataset.item
    wx.navigateTo({
      url: `/pages/attendance/attendance?classId=${item.teachingGroupId || this.data.classId || ''}`
        + `&className=${encodeURIComponent(this.data.className)}`
        + `&sessionId=${item.sessionId || ''}&focusStudentId=${item.studentId || ''}&autoOpen=1`
    })
  }
})
