const api = require('../../../utils/api.js')

Page({
  data: {
    logs: [],
    page: 1,
    size: 50,
    total: 0,
    loading: false
  },

  onShow() {
    this.setData({ page: 1 })
    this.loadLogs()
  },

  // 拉取后端操作日志
  loadLogs() {
    if (this.data.loading) return
    this.setData({ loading: true })
    wx.showLoading({ title: '加载中...', mask: true })

    api.pageOperationLogs({
      page: this.data.page,
      size: this.data.size
    }).then(pageData => {
      // OperationLogVO: id, operatorUserId, operatorUsername, operatorName,
      // moduleName, operationType, targetType, targetId, description,
      // beforeData, afterData, clientIp, createdAt
      const logs = (pageData.records || []).map(l => ({
        id: l.id,
        content: l.description || '',
        time: this.fmt(l.createdAt),
        operator: l.operatorName || l.operatorUsername || '系统',
        module: l.moduleName || '',
        type: l.operationType || '',
        targetType: l.targetType || '',
        targetId: l.targetId,
        clientIp: l.clientIp || ''
      }))
      this.setData({ logs, total: pageData.total || 0, loading: false })
      wx.hideLoading()
    }).catch(err => {
      wx.hideLoading()
      this.setData({ loading: false })
      wx.showToast({ title: (err && err.message) || '加载失败', icon: 'none' })
    })
  },

  // 格式化 ISO 时间为 yyyy-MM-dd HH:mm:ss
  fmt(iso) {
    if (!iso) return ''
    const s = String(iso).replace('T', ' ')
    return s.length > 19 ? s.substring(0, 19) : s
  },

  onBack() {
    const pages = getCurrentPages()
    if (pages.length > 1) wx.navigateBack()
    else wx.reLaunch({ url: '/pages/super_admin/super_admin' })
  }
})
