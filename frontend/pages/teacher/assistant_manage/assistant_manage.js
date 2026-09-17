const STORAGE_KEY = 'attendanceData'

Page({
  data: {
    assistantList: []
  },

  onShow() {
    this.loadData()
  },

  loadData() {
    // 助教数据暂时复用 students 中 role 为 assistant 的记录
    // 当前 mock 数据中没有助教独立存储，先用空数组占位
    // 实际项目中应从独立的助教数据源读取
    const data = wx.getStorageSync(STORAGE_KEY) || { students: [], assistants: [] }
    const assistants = data.assistants || []

    if (assistants.length > 0) {
      this.setData({ assistantList: assistants })
    } else {
      // Mock 助教数据，首次加载时写入
      const mockAssistants = [
        { id: 'T2023001', name: '刘助教', score: '' },
        { id: 'T2023002', name: '陈助教', score: '' },
        { id: 'T2023003', name: '黄助教', score: '' }
      ]
      data.assistants = mockAssistants
      wx.setStorageSync(STORAGE_KEY, data)
      this.setData({ assistantList: mockAssistants })
    }
  },

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
          // 本地持久化
          const data = wx.getStorageSync(STORAGE_KEY) || { assistants: [] }
          data.assistants = data.assistants.map((a, i) =>
            i === idx ? { ...a, score: newScore } : a
          )
          wx.setStorageSync(STORAGE_KEY, data)
          this.loadData()
          wx.showToast({ title: '打分成功', icon: 'success' })
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