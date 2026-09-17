const CLASS_KEY = 'classData'
const USER_KEY = 'userData'

Page({
  data: {
    teacherOptions: [],
    teacherIndex: 0,
    classList: [],
    selectedClassIds: []
  },

  onShow() { this.loadData() },

  ensureData() {
    // 初始化班级数据
    let classData = wx.getStorageSync(CLASS_KEY)
    if (!classData || !classData.length) {
      classData = [
        { id: 'C001', name: '周一-12节-茶园', teacher_id: '', teacher_name: '' },
        { id: 'C002', name: '周二-34节-果园', teacher_id: 'U001', teacher_name: '王教授' },
        { id: 'C003', name: '周三-56节-麦田', teacher_id: '', teacher_name: '' },
        { id: 'C004', name: '周四-78节-蔬菜大棚', teacher_id: 'U002', teacher_name: '李老师' }
      ]
      wx.setStorageSync(CLASS_KEY, classData)
    }
    // 初始化教师数据
    let userData = wx.getStorageSync(USER_KEY)
    if (!userData || !userData.length) {
      userData = [
        { id: 'U001', username: 'teacher1', realName: '王教授', role: 'teacher', roleName: '教师', status: 'enabled', createTime: '2026-09-01' },
        { id: 'U002', username: 'teacher2', realName: '李老师', role: 'teacher', roleName: '教师', status: 'enabled', createTime: '2026-09-02' }
      ]
      wx.setStorageSync(USER_KEY, userData)
    }
  },

  loadData() {
    this.ensureData()
    const classData = wx.getStorageSync(CLASS_KEY) || []
    const userData = wx.getStorageSync(USER_KEY) || []
    const baseTeachers = userData.filter(u => u.role === 'teacher' && u.status === 'enabled')
    const teacherOptions = baseTeachers.map(t => t.realName)
    const idx = Math.min(this.data.teacherIndex, Math.max(0, teacherOptions.length - 1))
    const curTeacher = baseTeachers[idx]
    const teacherId = curTeacher ? curTeacher.id : ''
    const classList = classData.map(c => {
      const assigned = c.teacher_ids ? c.teacher_ids.indexOf(teacherId) >= 0 : (c.teacher_id === teacherId)
      return { ...c, isSelected: assigned }
    })
    const selectedClassIds = classList.filter(c => c.isSelected).map(c => c.id)
    this.setData({
      teacherOptions,
      teacherIndex: idx,
      classList,
      selectedClassIds
    })
  },

  onTeacherChange(e) {
    this.setData({ teacherIndex: e.detail.value })
    this.loadData()
  },

  onClassChange(e) {
    const checkedIds = e.detail.value || []
    const classList = this.data.classList.map(c => ({
      ...c,
      isSelected: checkedIds.indexOf(c.id) >= 0
    }))
    this.setData({ classList, selectedClassIds: checkedIds })
  },

  onConfirm() {
    const { teacherIndex, selectedClassIds, teacherOptions } = this.data
    if (selectedClassIds.length === 0) {
      wx.showToast({ title: '请选择至少一个班级', icon: 'none' })
      return
    }
    const userData = wx.getStorageSync(USER_KEY) || []
    const baseTeachers = userData.filter(u => u.role === 'teacher' && u.status === 'enabled')
    const curTeacher = baseTeachers[teacherIndex]
    if (!curTeacher) {
      wx.showToast({ title: '请选择教师', icon: 'none' })
      return
    }
    const teacherId = curTeacher.id
    const teacherName = curTeacher.realName
    const classData = wx.getStorageSync(CLASS_KEY) || []
    classData.forEach(c => {
      const isSelected = selectedClassIds.indexOf(c.id) >= 0
      const ids = c.teacher_ids || (c.teacher_id ? [c.teacher_id] : [])
      const existIdx = ids.indexOf(teacherId)
      if (isSelected && existIdx < 0) {
        ids.push(teacherId)
      } else if (!isSelected && existIdx >= 0) {
        ids.splice(existIdx, 1)
      }
      c.teacher_ids = ids
      c.teacher_id = ids[0] || ''
      c.teacher_name = ids.map(id => {
        const t = baseTeachers.find(bt => bt.id === id)
        return t ? t.realName : ''
      }).filter(Boolean).join('、')
    })
    wx.setStorageSync(CLASS_KEY, classData)
    const app = getApp()
    const userInfo = app.globalData.userInfo || wx.getStorageSync('userInfo') || {}
    const operator = userInfo.account || userInfo.realName || '超级管理员'
    app.addLog(operator, `为「${teacherName}」分配班级：${selectedClassIds.length} 个`, 'data')
    wx.showToast({ title: '分配成功', icon: 'success' })
    this.loadData()
  },

  onBack() {
    const pages = getCurrentPages()
    if (pages.length > 1) wx.navigateBack()
    else wx.reLaunch({ url: '/pages/super_admin/super_admin' })
  }
})