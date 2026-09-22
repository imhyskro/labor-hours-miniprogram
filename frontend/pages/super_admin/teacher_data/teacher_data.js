const api = require('../../../utils/api.js')

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
    wx.showLoading({ title: '加载中...', mask: true })
    api.pageUsers({ page: 1, size: 200 }).then(pageData => {
      // 仅保留 TEACHER 角色
      const teachers = (pageData.records || []).filter(u => u.roles && u.roles.indexOf('TEACHER') >= 0)

      // 为每位教师并行拉取负责班级
      const tasks = teachers.map(t => api.getUserClasses(t.id).catch(() => []))
      Promise.all(tasks).then(results => {
        const rawList = teachers.map((t, i) => {
          const classes = results[i] || []
          const managedClasses = classes.map(c => c.className || c.classCode || ('班级#' + c.id))
          return {
            id: t.id,
            name: t.realName || '',
            code: t.username || '',
            org: '',  // 后端 API 暂无所属学院字段
            managedClasses,
            status: t.status === 1 ? 'enabled' : 'disabled',
            statusText: t.status === 1 ? '启用' : '停用'
          }
        })
        this.setData({ rawList })
        this.applyFilter()
        wx.hideLoading()
      })
    }).catch(err => {
      wx.hideLoading()
      wx.showToast({ title: (err && err.message) || '加载失败', icon: 'none' })
    })
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

  // 导出 CSV
  onExport() {
    const list = this.data.list
    if (!list.length) {
      wx.showToast({ title: '暂无数据可导出', icon: 'none' })
      return
    }
    const header = ['姓名', '工号', '管理班级', '状态']
    const rows = list.map(item => [
      item.name || '',
      item.code || '',
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
