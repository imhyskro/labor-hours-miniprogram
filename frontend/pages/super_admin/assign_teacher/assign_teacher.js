const api = require('../../../utils/api.js')
const auth = require('../../../utils/auth.js')

Page({
  data: {
    teacherOptions: [],     // 教师列表（来自 /api/admin/users/page，role=TEACHER）
    teacherList: [],        // 完整教师对象
    teacherIndex: 0,
    classList: [],          // 全部班级（来自 /api/classes/list）
    selectedClassIds: []   // 当前教师已负责班级 ID 数组
  },

  onShow() { this.loadData() },

  loadData() {
    wx.showLoading({ title: '加载中...', mask: true })

    // 拉教师列表 + 班级列表
    Promise.all([
      api.pageUsers({ page: 1, size: 200 }),  // 一次取足够多教师
      api.listClasses()
    ]).then(([pageData, classes]) => {
      const teachers = (pageData.records || []).filter(u => u.roles && u.roles.indexOf('TEACHER') >= 0)
      const teacherOptions = teachers.map(t => t.realName || t.username)

      const classList = (classes || []).map(c => ({
        id: c.id,
        name: c.className || c.classCode || ('班级#' + c.id)
      }))

      const idx = Math.min(this.data.teacherIndex, Math.max(0, teacherOptions.length - 1))
      this.setData({
        teacherOptions,
        teacherList: teachers,
        teacherIndex: idx,
        classList
      }, () => {
        // 加载该教师已分配的班级
        this.loadTeacherClasses(idx)
      })
      wx.hideLoading()
    }).catch(err => {
      wx.hideLoading()
      wx.showToast({ title: (err && err.message) || '加载失败', icon: 'none' })
    })
  },

  // 拉取当前选中教师的负责班级
  loadTeacherClasses(teacherIndex) {
    const teacher = this.data.teacherList[teacherIndex]
    if (!teacher) {
      this.setData({ selectedClassIds: [] })
      return
    }
    api.getUserClasses(teacher.id).then(classes => {
      const ids = (classes || []).map(c => c.id)
      this.setData({ selectedClassIds: ids })
    }).catch(err => {
      this.setData({ selectedClassIds: [] })
      wx.showToast({ title: '加载已分配班级失败', icon: 'none' })
    })
  },

  onTeacherChange(e) {
    this.setData({ teacherIndex: e.detail.value })
    this.loadTeacherClasses(e.detail.value)
  },

  onClassChange(e) {
    const checkedIds = e.detail.value || []
    this.setData({ selectedClassIds: checkedIds })
  },

  onConfirm() {
    const { teacherIndex, teacherList, selectedClassIds } = this.data
    const teacher = teacherList[teacherIndex]
    if (!teacher) {
      wx.showToast({ title: '请选择教师', icon: 'none' })
      return
    }

    wx.showModal({
      title: '确认分配',
      content: `将教师「${teacher.realName || teacher.username}」负责的班级全量覆盖为 ${selectedClassIds.length} 个？`,
      confirmColor: '#4A6B3A',
      success: res => {
        if (!res.confirm) return
        wx.showLoading({ title: '提交中...', mask: true })
        api.setUserClasses(teacher.id, selectedClassIds).then(() => {
          wx.hideLoading()
          const app = getApp()
          const me = auth.getUserInfo()
          const operator = (me && (me.realName || me.username)) || '超级管理员'
          if (app && app.addLog) {
            app.addLog(operator, `更新「${teacher.realName || teacher.username}」负责班级：${selectedClassIds.length} 个`, 'data')
          }
          wx.showToast({ title: '分配成功', icon: 'success' })
          this.loadTeacherClasses(this.data.teacherIndex)
        }).catch(err => {
          wx.hideLoading()
          wx.showToast({ title: (err && err.message) || '分配失败', icon: 'none' })
        })
      }
    })
  },

  onBack() {
    const pages = getCurrentPages()
    if (pages.length > 1) wx.navigateBack()
    else wx.reLaunch({ url: '/pages/super_admin/super_admin' })
  }
})
