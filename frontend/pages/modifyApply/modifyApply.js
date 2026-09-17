const STORAGE_KEY = 'attendanceData'
// 3 天自动撤销阈值（ms），从助教完成修改开始计算
const AUTO_REVOKE_MS = 3 * 24 * 60 * 60 * 1000

Page({
  data: {
    className: '',
    studentFilter: null,
    studentName: '',
    originScore: '',
    list: [],
    countdownTimer: null
  },

  onLoad(options) {
    const className = decodeURIComponent(options.className || '')
    const studentId = options.studentId
    const studentName = decodeURIComponent(options.studentName || '')
    const originScore = options.originScore

    this.setData({
      className,
      studentFilter: studentId || null,
      studentName,
      originScore: originScore || ''
    })
  },

  // 每次展示页面：从本地缓存读取最新申请记录 + 启动倒计时
  onShow() {
    this.loadData()
    this.startCountdown()
  },

  onUnload() {
    this.clearCountdown()
  },

  // 从本地缓存读取申请记录并渲染
  loadData() {
    const data = wx.getStorageSync(STORAGE_KEY) || { students: [], applications: [] }
    const applications = data.applications || []

    let list = applications.map(item => ({ ...item }))
    // 计算每条 modified 记录的剩余天数（基于助教修改完成时间 modifiedTime）
    list = list.map(item => this.computeCountdown(item))

    // 从考勤页跳入，该学生没有申请记录则补一条空记录引导提交
    if (this.data.studentFilter) {
      const exists = list.find(a => a.studentId === this.data.studentFilter)
      if (!exists) {
        list.unshift({
          id: 'APP_NEW_' + this.data.studentFilter,
          studentId: this.data.studentFilter,
          studentName: this.data.studentName,
          college: '',
          major: '',
          originScore: this.data.originScore || '',
          currentScore: this.data.originScore || '',
          applyReason: '',
          status: 'draft',
          statusText: '待提交',
          applyTime: '—',
          modifiedTime: null,
          teacher: '—',
          revokeCountdown: ''
        })
      }
    }

    this.setData({ list })
  },

  // 计算单条记录倒计时（modified -> X天后自动撤销 / 已失效）
  computeCountdown(item) {
    item.revokeCountdown = ''
    if (item.status === 'modified' && item.modifiedTime) {
      const remainMs = AUTO_REVOKE_MS - (Date.now() - item.modifiedTime)
      if (remainMs <= 0) {
        item.status = 'rejected'
        item.statusText = '已失效'
        item.currentScore = item.originScore
        item.revokeCountdown = '已失效'
      } else {
        const remainDays = Math.ceil(remainMs / (24 * 60 * 60 * 1000))
        item.revokeCountdown = `${remainDays}天后自动撤销`
      }
    }
    return item
  },

  startCountdown() {
    this.clearCountdown()
    const timer = setInterval(() => this.refreshCountdown(), 60 * 1000)
    this.data.countdownTimer = timer
  },

  clearCountdown() {
    if (this.data.countdownTimer) {
      clearInterval(this.data.countdownTimer)
      this.data.countdownTimer = null
    }
  },

  // 刷新倒计时（每分钟触发一次）
  refreshCountdown() {
    const list = this.data.list.map(item => this.computeCountdown({ ...item }))
    this.setData({ list })
  },

  // 撤销申请：删除申请记录 + 学生 needApproval 置 false（考勤页标签立即消失）
  onRevoke(e) {
    const id = e.currentTarget.dataset.id
    wx.showModal({
      title: '确认撤销',
      content: '撤销后该申请将作废，是否继续？',
      confirmColor: '#4A6B3A',
      success: res => {
        if (!res.confirm) return

        const data = wx.getStorageSync(STORAGE_KEY) || { students: [], applications: [] }
        const app = (data.applications || []).find(a => a.id === id)
        // 1) 从申请列表移除该记录
        data.applications = (data.applications || []).filter(a => a.id !== id)
        // 2) 同步将该学生 needApproval 置 false
        if (app && data.students) {
          const sIdx = data.students.findIndex(s => s.id === app.studentId)
          if (sIdx >= 0) {
            data.students[sIdx].needApproval = false
          }
        }
        wx.setStorageSync(STORAGE_KEY, data)

        // 更新视图
        const list = this.data.list.filter(item => item.id !== id)
        this.setData({ list })
        wx.showToast({ title: '已撤销申请', icon: 'success' })
      }
    })
  },

  // 教师已同意 -> 跳转页面三，传学号 + autoOpen=1（弹键盘）
  onModify(e) {
    const item = e.currentTarget.dataset.item
    wx.navigateTo({
      url: `/pages/attendance/attendance?className=${encodeURIComponent(this.data.className)}&focusStudentId=${item.studentId}&autoOpen=1`
    })
  },

  // 草稿/待提交 -> 跳转编辑修改申请页（页面五）
  onSubmitApply(e) {
    const item = e.currentTarget.dataset.item
    wx.navigateTo({
      url: `/pages/modifyEdit/modifyEdit?studentId=${item.studentId}&studentName=${encodeURIComponent(item.studentName)}&originScore=${item.originScore}&className=${encodeURIComponent(this.data.className)}`
    })
  },

  // 清除角标（从页面二进入时调用）
  clearBadge() {
    const app = getApp()
    if (app) app.globalData.modifyBadge = 0
  }
})
