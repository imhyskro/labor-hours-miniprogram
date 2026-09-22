const app = getApp()

Page({
  data: {
    userInfo: {}
  },

  onLoad() {
    this.setData({
      userInfo: app.globalData.userInfo || wx.getStorageSync('userInfo') || {}
    })
  },

  onBack() {
    const pages = getCurrentPages()
    if (pages.length > 1) {
      wx.navigateBack()
    } else {
      wx.reLaunch({ url: '/pages/login/login' })
    }
  },

  onGoUserManage() {
    wx.navigateTo({ url: '/pages/super_admin/user_manage/user_manage' })
  },

  onGoAssignTeacher() {
    wx.navigateTo({ url: '/pages/super_admin/assign_teacher/assign_teacher' })
  },

  onGoDashboard() {
    wx.navigateTo({ url: '/pages/super_admin/dashboard/dashboard' })
  },

  onGoLogs() {
    wx.navigateTo({ url: '/pages/super_admin/logs/logs' })
  },

  onGoCompanyManage() {
    wx.navigateTo({ url: '/pages/super_admin/company_manage/company_manage' })
  }
})