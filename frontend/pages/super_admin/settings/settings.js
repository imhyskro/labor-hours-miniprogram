Page({
  getOperator() {
    const app = getApp()
    const userInfo = app.globalData.userInfo || wx.getStorageSync('userInfo') || {}
    return userInfo.account || userInfo.realName || '超级管理员'
  },

  onClearAll() {
    wx.showModal({
      title: '⚠️ 危险操作',
      content: '确认清空所有本地数据？此操作不可恢复！',
      confirmColor: '#E53935',
      success: res => {
        if (!res.confirm) return
        const operator = this.getOperator()
        wx.clearStorageSync()
        getApp().addLog(operator, '一键清空所有本地数据', 'system')
        wx.showToast({ title: '系统已重置', icon: 'success' })
        setTimeout(() => {
          wx.reLaunch({ url: '/pages/login/login' })
        }, 1500)
      }
    })
  },

  onResetDefault() {
    wx.showModal({
      title: '恢复默认配置',
      content: '将清空现有数据并恢复为初始数据，确认继续？',
      confirmColor: '#4A6B3A',
      success: res => {
        if (!res.confirm) return
        const operator = this.getOperator()
        wx.clearStorageSync()
        getApp().addLog(operator, '恢复系统默认配置', 'system')
        wx.showToast({ title: '已恢复默认', icon: 'success' })
      }
    })
  },

  onBack() {
    const pages = getCurrentPages()
    if (pages.length > 1) wx.navigateBack()
    else wx.reLaunch({ url: '/pages/super_admin/super_admin' })
  }
})