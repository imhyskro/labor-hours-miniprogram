const api = require('../../utils/api.js')

Page({
  data: {
    status: '',         // pending = 仅展示未打分
    pendingList: [],    // PendingScoreVO[]
    loading: false
  },

  onLoad(options) {
    const status = options.status || ''
    this.setData({ status })
    this.loadPendingList()
  },

  // 拉取待打分课次列表
  loadPendingList() {
    this.setData({ loading: true })
    wx.showLoading({ title: '加载中...', mask: true })
    api.listPendingScores().then(list => {
      wx.hideLoading()
      const pendingList = (list || []).map(item => ({
        sessionId: item.sessionId,
        classId: item.classId,
        className: item.className || '未命名班级',
        weekNo: item.weekNo || '',
        sessionDate: item.sessionDate || '',
        unscoredCount: item.unscoredCount || 0
      }))
      this.setData({ pendingList, loading: false })
    }).catch(err => {
      wx.hideLoading()
      this.setData({ loading: false })
      wx.showToast({ title: (err && err.message) || '加载失败', icon: 'none' })
    })
  },

  // 点击列表项 → 跳转考勤打分页
  onItemTap(e) {
    const idx = e.currentTarget.dataset.index
    const item = this.data.pendingList[idx]
    if (!item) return
    wx.navigateTo({
      url: `/pages/attendance/attendance?classId=${item.classId}&className=${encodeURIComponent(item.className)}`
    })
  }
})
