const STORAGE_KEY = 'studentData'

Page({
  data: {
    rawList: [],
    list: [],
    keyword: ''
  },

  onShow() {
    this.loadData()
  },

  loadData() {
    let rawList = wx.getStorageSync(STORAGE_KEY) || []
    if (!rawList.length) {
      rawList = [
        { name: '张伟',   code: '202301001', college: '农学院', major: '园艺技术',   totalHours: 48, score: '优' },
        { name: '李娜',   code: '202301002', college: '农学院', major: '园艺技术',   totalHours: 42, score: '良' },
        { name: '王强',   code: '202302015', college: '林学院', major: '森林资源',   totalHours: 36, score: '良' },
        { name: '赵敏',   code: '202302028', college: '林学院', major: '森林资源',   totalHours: 30, score: '中' },
        { name: '刘洋',   code: '202303007', college: '动物院', major: '畜牧兽医',   totalHours: 45, score: '优' },
        { name: '陈晨',   code: '202303019', college: '动物院', major: '畜牧兽医',   totalHours: 24, score: '中' },
        { name: '孙悦',   code: '202304003', college: '食品院', major: '食品科学',   totalHours: 38, score: '良' },
        { name: '周杰',   code: '202304011', college: '食品院', major: '食品科学',   totalHours: 50, score: '优' }
      ]
      wx.setStorageSync(STORAGE_KEY, rawList)
    }
    this.setData({ rawList })
    this.applyFilter()
  },

  applyFilter() {
    const kw = (this.data.keyword || '').trim().toLowerCase()
    const list = !kw
      ? this.data.rawList
      : this.data.rawList.filter(item =>
          (item.name && String(item.name).toLowerCase().indexOf(kw) >= 0) ||
          (item.code && String(item.code).toLowerCase().indexOf(kw) >= 0)
        )
    this.setData({ list })
  },

  onSearch(e) {
    this.setData({ keyword: e.detail.value })
    this.applyFilter()
  },

  onImport() {
    wx.chooseMessageFile({
      count: 1,
      type: 'file',
      extension: ['xlsx', 'xls', 'csv'],
      success: res => {
        const file = res.tempFiles && res.tempFiles[0]
        if (!file) return
        wx.showLoading({ title: '解析中...' })
        this.parseExcel(file.path)
      },
      fail: () => {}
    })
  },

  parseExcel(filePath) {
    const parsedRows = []
    const merged = this.data.rawList.concat(parsedRows)
    wx.setStorageSync(STORAGE_KEY, merged)
    wx.hideLoading()
    wx.showToast({ title: '已追加 ' + parsedRows.length + ' 条', icon: 'none' })
    this.loadData()
  },

  onExport() {
    const list = this.data.list
    if (!list.length) {
      wx.showToast({ title: '暂无数据可导出', icon: 'none' })
      return
    }
    const header = ['姓名', '学号', '学院', '专业', '累计学时', '成绩']
    const rows = list.map(item => [
      item.name || '',
      item.code || '',
      item.college || '',
      item.major || '',
      item.totalHours || 0,
      item.score || ''
    ])
    const csv = [header, ...rows].map(r => r.join(',')).join('\n')
    const fs = wx.getFileSystemManager()
    const filePath = `${wx.env.USER_DATA_PATH}/student_export_${Date.now()}.csv`
    fs.writeFile({
      filePath,
      data: String.fromCharCode(0xFEFF) + csv,
      encoding: 'utf8',
      success: () => {
        wx.showModal({
          title: '导出成功',
          content: '已生成 CSV 文件，是否打开？',
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
      fail: () => wx.showToast({ title: '导出失败', icon: 'none' })
    })
  },

  onBack() {
    const pages = getCurrentPages()
    if (pages.length > 1) wx.navigateBack()
    else wx.reLaunch({ url: '/pages/super_admin/super_admin' })
  }
})