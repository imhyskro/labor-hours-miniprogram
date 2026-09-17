const app = getApp()
const STORAGE_KEY = 'attendanceData'

Page({
  data: {
    userInfo: {},
    approvalCount: 0
  },

  onLoad() {
    const userInfo = app.globalData.userInfo || wx.getStorageSync('userInfo') || {}
    this.setData({ userInfo })
  },

  // 每次显示页面时刷新审批角标（从本地缓存读取 pending 状态申请数量）
  onShow() {
    const data = wx.getStorageSync(STORAGE_KEY) || { applications: [] }
    const pending = (data.applications || []).filter(a => a.status === 'pending')
    this.setData({ approvalCount: pending.length })
  },

  // 返回登录页
  onBack() {
    const pages = getCurrentPages()
    if (pages.length > 1) {
      wx.navigateBack()
    } else {
      wx.reLaunch({ url: '/pages/login/login' })
    }
  },

  onGoStudentList() {
    wx.navigateTo({ url: '/pages/teacher/student_list/student_list' })
  },

  onGoClassManage() {
    wx.navigateTo({ url: '/pages/teacher/class_manage/class_manage' })
  },

  onGoAssistantManage() {
    wx.navigateTo({ url: '/pages/teacher/assistant_manage/assistant_manage' })
  },

  onGoDataImport() {
    wx.navigateTo({ url: '/pages/teacher/data_import/data_import' })
  },

  onGoApproval() {
    wx.navigateTo({ url: '/pages/teacher/approval/approval' })
  }
})
