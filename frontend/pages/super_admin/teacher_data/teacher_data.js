const TEACHER_KEY = 'teacherData'
const CLASS_KEY = 'classData'

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
    const teachers = wx.getStorageSync(TEACHER_KEY) || []
    const classes = wx.getStorageSync(CLASS_KEY) || []
    const rawList = teachers.map(t => {
      const managed = classes.filter(c => {
        if (c.teacher_ids && c.teacher_ids.length) return c.teacher_ids.indexOf(t.id) >= 0
        return c.teacher_id === t.id
      }).map(c => c.name)
      return {
        ...t,
        managedClasses: managed,
        statusText: t.status === 'enabled' ? '启用' : '未启用'
      }
    })
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
    wx.setStorageSync(TEACHER_KEY, merged)
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
    const header = ['姓名', '工号', '所属学院', '管理班级', '状态']
    const rows = list.map(item => [
      item.name || '',
      item.code || '',
      item.org || '',
      (item.managedClasses || []).join('、'),
      item.statusText || ''
    ])
    const csv = [header, ...rows].map(r => r.join(',')).join('\n')
    const fs = wx.getFileSystemManager()
    const filePath = `${wx.env.USER_DATA_PATH}/teacher_export_${Date.now()}.csv`
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