const api = require('../../../utils/api.js')

Page({
  data: {
    classOptions: ['全部班级'],
    classIndex: 0,
    filteredList: [],
    rawList: [],
    loading: false
  },

  onShow() {
    this.loadData()
  },

  loadData() {
    if (this.data.loading) return
    this.setData({ loading: true })
    wx.showLoading({ title: '加载中...', mask: true })

    // 教师角色：后端自动限制为该教师负责的班级，无需传 teacherId
    api.masterList({ page: 1, size: 500 }).then(pageData => {
      const rawList = (pageData.records || []).map(r => ({
        id: r.id,
        studentId: r.studentId,
        name: r.name,
        className: r.className || '',
        originalMajor: r.originalMajor || '',
        identity: r.identity || '',
        isAssistant: r.isAssistant,
        // 当前学时/分数：后端暂无对应接口，留空
        score: ''
      }))

      // 提取唯一班级
      const classSet = new Set()
      rawList.forEach(s => { if (s.className) classSet.add(s.className) })
      const classOptions = ['全部班级', ...Array.from(classSet)]

      const idx = Math.min(this.data.classIndex, classOptions.length - 1)
      let filteredList = rawList
      if (idx > 0) {
        const selectedClass = classOptions[idx]
        filteredList = rawList.filter(s => s.className === selectedClass)
      }

      this.setData({
        classOptions,
        classIndex: idx,
        filteredList,
        rawList,
        loading: false
      })
      wx.hideLoading()
    }).catch(err => {
      wx.hideLoading()
      this.setData({ loading: false })
      wx.showToast({ title: (err && err.message) || '加载失败', icon: 'none' })
    })
  },

  onClassChange(e) {
    this.setData({ classIndex: e.detail.value })
    const idx = e.detail.value
    if (idx === 0) {
      this.setData({ filteredList: this.data.rawList })
    } else {
      const selectedClass = this.data.classOptions[idx]
      const filteredList = this.data.rawList.filter(s => s.className === selectedClass)
      this.setData({ filteredList })
    }
  },

  onBack() {
    const pages = getCurrentPages()
    if (pages.length > 1) wx.navigateBack()
    else wx.reLaunch({ url: '/pages/teacher/teacher' })
  }
})
