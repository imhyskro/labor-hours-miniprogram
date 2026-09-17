const LOG_KEY = 'operationLogs'

Page({
  data: {
    logs: []
  },

  onShow() {
    this.loadLogs()
  },

  loadLogs() {
    const logs = wx.getStorageSync(LOG_KEY) || []
    this.setData({ logs })
  },

  onBack() {
    const pages = getCurrentPages()
    if (pages.length > 1) wx.navigateBack()
    else wx.reLaunch({ url: '/pages/super_admin/super_admin' })
  }
})