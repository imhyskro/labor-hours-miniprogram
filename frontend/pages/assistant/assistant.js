const app = getApp()
const { classOptions } = require('../../utils/mockData.js')

Page({
  data: {
    userInfo: {},
    dayIndex: 0,
    periodIndex: 0,
    locationIndex: 0,
    days: classOptions.days,
    periods: classOptions.periods,
    locations: classOptions.locations
  },

  onLoad() {
    const userInfo = app.globalData.userInfo || wx.getStorageSync('userInfo') || {}
    this.setData({ userInfo })
  },

  onDayChange(e) {
    this.setData({ dayIndex: e.detail.value })
  },

  onPeriodChange(e) {
    this.setData({ periodIndex: e.detail.value })
  },

  onLocationChange(e) {
    this.setData({ locationIndex: e.detail.value })
  },

  onEnterClass() {
    const { dayIndex, periodIndex, locationIndex, days, periods, locations } = this.data
    const day = days[dayIndex]
    const period = periods[periodIndex]
    const location = locations[locationIndex]
    const className = `${day}-${period}-${location}`

    wx.navigateTo({
      url: `/pages/classCenter/classCenter?day=${day}&period=${period}&location=${location}&className=${encodeURIComponent(className)}`
    })
  },

  // 自定义返回：有上一页则返回，否则跳登录页兜底
  onBack() {
    const pages = getCurrentPages()
    if (pages.length > 1) {
      wx.navigateBack()
    } else {
      wx.reLaunch({ url: '/pages/login/login' })
    }
  }
})
