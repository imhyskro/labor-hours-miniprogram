const api = require('../../../utils/api.js')
const auth = require('../../../utils/auth.js')

Page({
  data: {
    isSuperAdmin: false
  },

  onLoad() {
    this.setData({ isSuperAdmin: auth.isSuperAdmin() })
  },

  // 下载学生导入模板
  onDownloadStudentTemplate() {
    this.downloadTemplate('students')
  },

  // 下载助教导入模板
  onDownloadAssistantTemplate() {
    this.downloadTemplate('assistants')
  },

  downloadTemplate(type) {
    wx.showLoading({ title: '下载模板...', mask: true })
    const downloader = type === 'students'
      ? api.downloadStudentTemplate
      : api.downloadAssistantTemplate

    downloader.call(api).then(res => {
      // res.data 为 arraybuffer
      const buf = res.data
      if (!buf) {
        wx.hideLoading()
        wx.showToast({ title: '模板下载失败', icon: 'none' })
        return
      }
      const fs = wx.getFileSystemManager()
      const filePath = `${wx.env.USER_DATA_PATH}/${type}_template.xlsx`
      fs.writeFile({
        filePath,
        data: buf,
        encoding: 'binary',
        success: () => {
          wx.hideLoading()
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
        fail: () => {
          wx.hideLoading()
          wx.showToast({ title: '模板写入失败', icon: 'none' })
        }
      })
    }).catch(err => {
      wx.hideLoading()
      wx.showToast({ title: (err && err.message) || '模板下载失败', icon: 'none' })
    })
  },

  // 导入学生数据
  onImportStudents() {
    this.pickAndImport('students')
  },

  // 导入助教数据
  onImportAssistants() {
    this.pickAndImport('assistants')
  },

  pickAndImport(type) {
    if (!this.data.isSuperAdmin) {
      wx.showToast({ title: '仅超级管理员可导入', icon: 'none' })
      return
    }
    wx.chooseMessageFile({
      count: 1,
      type: 'file',
      extension: ['xlsx', 'xls'],
      success: res => {
        const file = res.tempFiles && res.tempFiles[0]
        if (!file) return
        this.doImport(type, file.path)
      },
      fail: () => {
        wx.showToast({ title: '未选择文件', icon: 'none' })
      }
    })
  },

  doImport(type, filePath) {
    wx.showLoading({ title: '导入中...', mask: true })
    const importer = type === 'students' ? api.importStudents : api.importAssistants
    importer.call(api, filePath).then(data => {
      wx.hideLoading()
      const total = data.total || 0
      const success = data.successCount || 0
      const fail = data.failCount || 0
      let msg = `共 ${total} 条，成功 ${success} 条`
      if (fail > 0) {
        msg += `，失败 ${fail} 条`
        const errs = (data.errors || []).slice(0, 5)
        wx.showModal({
          title: '部分导入失败',
          content: msg + '\n' + errs.map(e => `行${e.row} ${e.studentId || ''}：${e.reason}`).join('\n'),
          showCancel: false
        })
      } else {
        wx.showToast({ title: msg, icon: 'success' })
      }
    }).catch(err => {
      wx.hideLoading()
      wx.showToast({ title: (err && err.message) || '导入失败', icon: 'none' })
    })
  },

  onBack() {
    const pages = getCurrentPages()
    if (pages.length > 1) wx.navigateBack()
    else wx.reLaunch({ url: '/pages/teacher/teacher' })
  }
})
