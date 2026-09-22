const api = require('../../../utils/api.js')

Page({
  data: {
    assistantList: [],
    page: 1,
    size: 200,
    total: 0,
    loading: false
  },

  onShow() {
    this.loadData()
  },

  // 使用后端助教分页接口（/api/assistants/page）
  // AssistantVO: id、studentId、name、gender、originalMajor、companyName、
  // classCode、className、studentNoInClass、fullNo、hasAccount、
  // assignedClassCount、assignedClassNames
  loadData() {
    if (this.data.loading) return
    this.setData({ loading: true })
    wx.showLoading({ title: '加载中...', mask: true })

    api.pageAssistants(this.data.page, this.data.size).then(pageData => {
      const list = (pageData.records || []).map(a => ({
        id: a.id,
        studentId: a.studentId,
        name: a.name,
        className: a.className || '',
        originalMajor: a.originalMajor || '',
        companyName: a.companyName || '',
        assignedClassCount: a.assignedClassCount || 0,
        assignedClassNames: a.assignedClassNames || '',
        hasAccount: a.hasAccount
      }))
      this.setData({ assistantList: list, total: pageData.total || 0, loading: false })
      wx.hideLoading()
    }).catch(err => {
      wx.hideLoading()
      this.setData({ loading: false })
      wx.showToast({ title: (err && err.message) || '加载失败', icon: 'none' })
    })
  },

  // 导出助教 Excel
  onExport() {
    wx.showLoading({ title: '导出中...', mask: true })
    api.exportAssistants().then(res => {
      wx.hideLoading()
      this.saveAndOpenExcel(res.data, '助教数据.xlsx')
    }).catch(err => {
      wx.hideLoading()
      wx.showToast({ title: (err && err.message) || '导出失败', icon: 'none' })
    })
  },

  // 保存并打开 Excel 文件
  saveAndOpenExcel(buf, fileName) {
    if (!buf) {
      wx.showToast({ title: '文件为空', icon: 'none' })
      return
    }
    const fs = wx.getFileSystemManager()
    const filePath = `${wx.env.USER_DATA_PATH}/${fileName}`
    fs.writeFile({
      filePath,
      data: buf,
      encoding: 'binary',
      success: () => {
        wx.showModal({
          title: '导出成功',
          content: '是否打开文件？',
          success: r => {
            if (r.confirm) {
              wx.openDocument({
                filePath,
                showMenu: true,
                fail: () => wx.showToast({ title: '请在聊天中查看文件', icon: 'none' })
              })
            }
          }
        })
      },
      fail: () => wx.showToast({ title: '文件保存失败', icon: 'none' })
    })
  },

  // 打分：后端无对应接口，暂存本地缓存占位
  onScoreTap(e) {
    const idx = e.currentTarget.dataset.index
    const assistant = this.data.assistantList[idx]
    wx.showModal({
      title: '打分',
      editable: true,
      placeholderText: '请输入分数',
      content: String(assistant.score || ''),
      success: res => {
        if (res.confirm && res.content && res.content.trim()) {
          const newScore = res.content.trim()
          const scores = wx.getStorageSync('assistantScores') || {}
          scores[assistant.studentId || assistant.id] = newScore
          wx.setStorageSync('assistantScores', scores)
          const assistantList = this.data.assistantList.map((a, i) =>
            i === idx ? { ...a, score: newScore } : a
          )
          this.setData({ assistantList })
          wx.showToast({ title: '已保存（本地）', icon: 'success' })
        }
      }
    })
  },

  onBack() {
    const pages = getCurrentPages()
    if (pages.length > 1) wx.navigateBack()
    else wx.reLaunch({ url: '/pages/teacher/teacher' })
  }
})
