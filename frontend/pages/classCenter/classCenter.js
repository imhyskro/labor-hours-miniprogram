Page({
  data: {
    classId: null,
    className: '',
    modifyBadge: 0
  },

  onLoad(options) {
    const classId = options.classId ? Number(options.classId) : null
    const className = decodeURIComponent(options.className || '')
    this.setData({ classId, className })
  },

  onShow() {
    const app = getApp()
    const badge = (app && app.globalData && app.globalData.modifyBadge) || 0
    this.setData({ modifyBadge: badge })
  },

  // 进入考勤页：携带 classId 与 className
  onGoAttendance() {
    const { classId, className } = this.data
    wx.navigateTo({
      url: `/pages/attendance/attendance?classId=${classId || ''}&className=${encodeURIComponent(className)}`
    })
  },

  onGoModifyApply() {
    const app = getApp()
    if (app && app.globalData) app.globalData.modifyBadge = 0
    this.setData({ modifyBadge: 0 })

    const { classId, className } = this.data
    wx.navigateTo({
      url: `/pages/modifyApply/modifyApply?classId=${classId || ''}&className=${encodeURIComponent(className)}`
    })
  }
})
