const api = require('../../../utils/api.js')

Page({
  data: {
    list: [],
    keyword: '',
    page: 1,
    size: 200
  },

  onShow() {
    this.loadData()
  },

  loadData() {
    wx.showLoading({ title: '加载中...', mask: true })
    api.pageAssistants(this.data.page, this.data.size, this.data.keyword).then(pageData => {
      wx.hideLoading()
      const records = (pageData.records || pageData.list || []).map(r => ({
        id: r.id,
        name: r.name || r.realName || '',
        studentId: r.studentId || r.studentNo || '',
        college: r.college || r.companyName || '',
        className: r.className || '',
        identity: '助教',
        status: r.status === 1 || r.status === 'enabled' ? '启用' : '停用'
      }))
      this.setData({ list: records })
    }).catch(err => {
      wx.hideLoading()
      wx.showToast({ title: (err && err.message) || '加载失败', icon: 'none' })
    })
  },

  onSearch(e) {
    this.setData({ keyword: e.detail.value })
  },

  onSearchConfirm() {
    this.loadData()
  },

  // 导入助教数据
  onImport() {
    wx.chooseMessageFile({
      count: 1,
      type: 'file',
      extension: ['xlsx', 'xls'],
      success: res => {
        const file = res.tempFiles && res.tempFiles[0]
        if (!file) return
        this.doImport(file.path)
      },
      fail: () => {}
    })
  },

  doImport(filePath) {
    wx.showLoading({ title: '导入中...', mask: true })
    api.importAssistants(filePath).then(data => {
      wx.hideLoading()
      const total = data.total || 0
      const success = data.successCount || 0
      const fail = data.failCount || 0
      let msg = `共 ${total} 条，成功 ${success} 条`
      if (fail > 0) {
        msg += `，失败 ${fail} 条`
        const errs = (data.errors || []).slice(0, 3)
        wx.showModal({
          title: '部分导入失败',
          content: msg + '\n' + errs.map(e => `行${e.row} ${e.studentId || ''}：${e.reason}`).join('\n'),
          showCancel: false
        })
      } else {
        wx.showToast({ title: msg, icon: 'success' })
      }
      this.loadData()
    }).catch(err => {
      wx.hideLoading()
      wx.showToast({ title: (err && err.message) || '导入失败', icon: 'none' })
    })
  },

  // 导出助教数据
  onExport() {
    wx.showLoading({ title: '导出中...', mask: true })
    api.exportAssistants().then(res => {
      wx.hideLoading()
      const buf = res.data
      if (!buf) { wx.showToast({ title: '导出失败', icon: 'none' }); return }
      const fs = wx.getFileSystemManager()
      const filePath = `${wx.env.USER_DATA_PATH}/assistants_export.xlsx`
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
        fail: () => wx.showToast({ title: '文件写入失败', icon: 'none' })
      })
    }).catch(err => {
      wx.hideLoading()
      wx.showToast({ title: (err && err.message) || '导出失败', icon: 'none' })
    })
  },

  // 下载助教导入模板
  onDownloadTemplate() {
    wx.showLoading({ title: '下载模板...', mask: true })
    api.downloadAssistantTemplate().then(res => {
      wx.hideLoading()
      const buf = res.data
      if (!buf) { wx.showToast({ title: '模板下载失败', icon: 'none' }); return }
      const fs = wx.getFileSystemManager()
      const filePath = `${wx.env.USER_DATA_PATH}/assistant_template.xlsx`
      fs.writeFile({
        filePath,
        data: buf,
        encoding: 'binary',
        success: () => {
          wx.showModal({
            title: '模板已下载',
            content: '是否打开模板文件？',
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
        fail: () => wx.showToast({ title: '模板写入失败', icon: 'none' })
      })
    }).catch(err => {
      wx.hideLoading()
      wx.showToast({ title: (err && err.message) || '模板下载失败', icon: 'none' })
    })
  },

  onBack() {
    const pages = getCurrentPages()
    if (pages.length > 1) wx.navigateBack()
    else wx.reLaunch({ url: '/pages/super_admin/super_admin' })
  }
})
