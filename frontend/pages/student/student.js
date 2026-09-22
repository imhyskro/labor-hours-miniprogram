const app = getApp()

Page({
  data: {
    userInfo: {}
  },

  onLoad() {
    this.setData({
      userInfo: app.globalData.userInfo || wx.getStorageSync('userInfo') || {}
    })
  }
})
