const api = require('../../utils/api.js')

Page({
  data: {
    studentId: '',
    studentName: '',
    studentNo: '',
    college: '',
    major: '',
    grade: '',
    sessionId: '',
    originScore: '',
    className: '',
    applyReason: '',
    submitting: false
  },

  onLoad(options) {
    this.setData({
      studentId: options.studentId || '',
      studentName: decodeURIComponent(options.studentName || ''),
      studentNo: decodeURIComponent(options.studentNo || ''),
      college: decodeURIComponent(options.college || ''),
      major: decodeURIComponent(options.major || ''),
      grade: decodeURIComponent(options.grade || ''),
      sessionId: options.sessionId || '',
      originScore: options.originScore || '',
      className: decodeURIComponent(options.className || '')
    })
  },

  onReasonInput(e) {
    this.setData({ applyReason: e.detail.value })
  },

  onSubmit() {
    const { studentId, sessionId, applyReason, submitting } = this.data
    if (submitting) return
    if (!studentId || !sessionId) {
      wx.showToast({ title: '缺少课次或学生信息', icon: 'none' })
      return
    }
    if (!applyReason.trim()) {
      wx.showToast({ title: '请填写申请理由', icon: 'none' })
      return
    }

    wx.showModal({
      title: '确认提交',
      content: '确认提交本次修改申请？',
      confirmColor: '#4A6B3A',
      success: res => {
        if (!res.confirm) return
        this.setData({ submitting: true })
        wx.showLoading({ title: '提交中...', mask: true })
        api.submitModificationRequest(sessionId, studentId, applyReason.trim())
          .then(() => {
            wx.hideLoading()
            wx.showToast({ title: '申请已提交', icon: 'success' })
            setTimeout(() => wx.navigateBack(), 1000)
          })
          .catch(err => {
            wx.hideLoading()
            this.setData({ submitting: false })
            wx.showToast({ title: (err && err.message) || '提交失败', icon: 'none' })
          })
      }
    })
  }
})
