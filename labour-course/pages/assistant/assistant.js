const app = getApp()
const api = require('../../utils/api.js')
const auth = require('../../utils/auth.js')

Page({
  data: {
    userInfo: {},
    classList: [],       // 可访问班级完整对象（含 hasPending）
    classOptions: [],    // 班级下拉选项名
    classIndex: 0,
    currentClass: {},    // 当前选中班级（用于预览卡片 + 红点）
    loading: false,
    fromRemind: false
  },

  onLoad(options) {
    if (!auth.isAssistantOnly()) {
      auth.clearAuth()
      wx.reLaunch({ url: '/pages/login/login' })
      return
    }
    const userInfo = app.globalData.userInfo || wx.getStorageSync('userInfo') || {}
    this.setData({ userInfo, fromRemind: options.from === 'remind' })
    this.loadClasses()
  },

  // 从考勤页返回时：若打过分则重新检查红点
  onShow() {
    if (app.globalData.classListDirty) {
      app.globalData.classListDirty = false
      // 只重新检查红点，不重拉班级列表
      this.checkAllPending(this.data.classList)
    }
  },

  // 拉取当前助教可访问的考勤班级
  loadClasses() {
    if (this.data.loading) return
    this.setData({ loading: true })
    wx.showLoading({ title: '加载中...', mask: true })

    api.listAttendanceClasses().then(classes => {
      const list = (classes || []).map(c => ({
        id: c.id,
        className: String(c.className || c.classCode || ('班级#' + c.id))
          .replace(/第(\d+)周/g, '周$1'),
        companyName: c.companyName || '',
        week: c.week,
        startSession: c.startSession,
        endSession: c.endSession,
        hasPending: false
      }))
      const classOptions = list.map(c => c.className)
      this.setData({
        classList: list,
        classOptions,
        classIndex: 0,
        currentClass: list[0] || {},
        loading: false
      })
      wx.hideLoading()
      this.checkAllPending(list)
    }).catch(err => {
      wx.hideLoading()
      this.setData({ loading: false })
      wx.showToast({ title: (err && err.message) || '加载班级失败', icon: 'none' })
    })
  },

  // 并发检查每个班级的红点状态（打分后返回时也会调）
  checkAllPending(list) {
    list.forEach((cls, idx) => {
      api.checkClassPending(cls.id).then(has => {
        const updates = { [`classList[${idx}].hasPending`]: has }
        // 若更新的是当前选中班级，同步 currentClass 的红点
        if (idx === this.data.classIndex) {
          updates.currentClass = Object.assign({}, this.data.currentClass, { hasPending: has })
        }
        this.setData(updates)
      })
    })
  },

  // 滑动选择器切换班级（原生 picker）
  onClassChange(e) {
    const idx = e.detail.value
    const cls = this.data.classList[idx] || {}
    this.setData({ classIndex: idx, currentClass: cls })
  },

  // 进入考勤管理页
  onEnterClass() {
    const cls = this.data.currentClass
    if (!cls || !cls.id) {
      wx.showToast({ title: '请先选择班级', icon: 'none' })
      return
    }
    wx.navigateTo({
      url: `/pages/attendance/attendance?classId=${cls.id}&className=${encodeURIComponent(cls.className)}`
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
