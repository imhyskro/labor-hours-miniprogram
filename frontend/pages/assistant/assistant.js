const app = getApp()
const api = require('../../utils/api.js')

Page({
  data: {
    userInfo: {},
    classList: [],       // 可访问班级完整对象
    classOptions: [],    // 班级下拉选项名
    classIndex: 0,
    loading: false
  },

  onLoad() {
    const userInfo = app.globalData.userInfo || wx.getStorageSync('userInfo') || {}
    this.setData({ userInfo })
    this.loadClasses()
  },

  // 拉取当前助教可访问的考勤班级
  loadClasses() {
    if (this.data.loading) return
    this.setData({ loading: true })
    wx.showLoading({ title: '加载中...', mask: true })

    api.listAttendanceClasses().then(classes => {
      const list = (classes || []).map(c => ({
        id: c.id,
        className: c.className || c.classCode || ('班级#' + c.id),
        companyName: c.companyName || '',
        week: c.week,
        startSession: c.startSession,
        endSession: c.endSession
      }))
      const classOptions = list.map(c => c.className)
      this.setData({
        classList: list,
        classOptions,
        classIndex: 0,
        loading: false
      })
      wx.hideLoading()
    }).catch(err => {
      wx.hideLoading()
      this.setData({ loading: false })
      wx.showToast({ title: (err && err.message) || '加载班级失败', icon: 'none' })
    })
  },

  onClassChange(e) {
    this.setData({ classIndex: e.detail.value })
  },

  // 进入班级中心，携带 classId 与 className
  onEnterClass() {
    const { classList, classIndex } = this.data
    const cls = classList[classIndex]
    if (!cls) {
      wx.showToast({ title: '请先选择班级', icon: 'none' })
      return
    }
    wx.navigateTo({
      url: `/pages/classCenter/classCenter?classId=${cls.id}&className=${encodeURIComponent(cls.className)}`
    })
  },

  // 自定义返回
  onBack() {
    const pages = getCurrentPages()
    if (pages.length > 1) {
      wx.navigateBack()
    } else {
      wx.reLaunch({ url: '/pages/login/login' })
    }
  }
})
