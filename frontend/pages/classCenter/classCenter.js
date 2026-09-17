Page({
  data: {
    day: '',
    period: '',
    location: '',
    className: '',
    modifyBadge: 0
  },

  onLoad(options) {
    const { day, period, location, className } = options
    this.setData({
      day: day || '周一',
      period: period || '34节',
      location: location || '果园',
      className: decodeURIComponent(className || `${day}-${period}-${location}`)
    })
  },

  onShow() {
    // 每次显示页面时刷新角标
    const app = getApp()
    const badge = (app && app.globalData && app.globalData.modifyBadge) || 0
    this.setData({ modifyBadge: badge })
  },

  onGoAttendance() {
    const { day, period, location, className } = this.data
    wx.navigateTo({
      url: `/pages/attendance/attendance?day=${day}&period=${period}&location=${location}&className=${encodeURIComponent(className)}`
    })
  },

  onGoModifyApply() {
    // 进入页面四，清除角标（模拟已读）
    const app = getApp()
    if (app && app.globalData) app.globalData.modifyBadge = 0
    this.setData({ modifyBadge: 0 })

    const { day, period, location, className } = this.data
    wx.navigateTo({
      url: `/pages/modifyApply/modifyApply?day=${day}&period=${period}&location=${location}&className=${encodeURIComponent(className)}`
    })
  }
})
