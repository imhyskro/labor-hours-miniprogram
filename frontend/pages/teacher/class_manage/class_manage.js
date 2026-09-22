const api = require('../../../utils/api.js')
const auth = require('../../../utils/auth.js')

Page({
  data: {
    classOptions: [],
    classList: [],
    classIndex: 0,
    filteredList: [],
    isSuperAdmin: false,
    // 公司下拉（超管新增班级用）
    companyList: [],
    companyOptions: [],
    // 弹窗
    modalVisible: false,
    modalMode: 'add', // add | edit
    editClassId: null,
    form: {
      companyIndex: 0,
      week: '',
      startSession: '',
      endSession: '',
      status: 1
    }
  },

  onShow() {
    this.setData({ isSuperAdmin: auth.isSuperAdmin() })
    this.loadClasses()
    if (auth.isSuperAdmin()) this.loadCompanies()
  },

  // 拉取公司列表（仅超管）
  loadCompanies() {
    api.listCompanies().then(rows => {
      const list = (rows || []).map(c => ({ id: c.id, name: c.name || '' }))
      this.setData({
        companyList: list,
        companyOptions: list.map(c => c.name)
      })
    }).catch(err => {
      // 教师无权访问公司列表时静默失败
      console.warn('loadCompanies failed:', err && err.message)
    })
  },

  // 拉取班级列表
  loadClasses() {
    wx.showLoading({ title: '加载中...', mask: true })
    api.listClasses().then(classes => {
      const list = (classes || []).map(c => ({
        id: c.id,
        className: c.className || c.classCode || ('班级#' + c.id),
        classCode: c.classCode,
        companyId: c.companyId,
        companyName: c.companyName,
        week: c.week,
        startSession: c.startSession,
        endSession: c.endSession,
        status: c.status,
        studentCount: c.studentCount || 0
      }))
      const classOptions = list.map(c => c.className)
      const idx = Math.min(this.data.classIndex, Math.max(0, list.length - 1))
      this.setData({ classList: list, classOptions, classIndex: idx }, () => {
        if (list.length) this.loadStudents(list[idx].id)
        else this.setData({ filteredList: [] })
      })
      wx.hideLoading()
    }).catch(err => {
      wx.hideLoading()
      wx.showToast({ title: (err && err.message) || '加载失败', icon: 'none' })
    })
  },

  // 拉取班级学生
  loadStudents(classId) {
    api.getClassStudents(classId).then(students => {
      const filteredList = (students || []).map(s => ({
        id: s.id,
        studentId: s.studentId,
        name: s.name,
        originalMajor: s.originalMajor || '',
        gender: s.gender,
        isAssistant: s.isAssistant,
        score: ''
      }))
      this.setData({ filteredList })
    }).catch(err => {
      wx.showToast({ title: (err && err.message) || '加载学生失败', icon: 'none' })
      this.setData({ filteredList: [] })
    })
  },

  onClassChange(e) {
    const idx = e.detail.value
    this.setData({ classIndex: idx })
    const cls = this.data.classList[idx]
    if (cls) this.loadStudents(cls.id)
  },

  // ===== 新增班级弹窗 =====
  onAddClass() {
    this.setData({
      modalVisible: true,
      modalMode: 'add',
      editClassId: null,
      form: {
        companyIndex: 0,
        week: '',
        startSession: '',
        endSession: '',
        status: 1
      }
    })
  },

  // ===== 编辑班级弹窗 =====
  onEditClass() {
    if (!this.data.isSuperAdmin) {
      wx.showToast({ title: '仅超级管理员可编辑班级', icon: 'none' })
      return
    }
    const cls = this.data.classList[this.data.classIndex]
    if (!cls) return
    const companyIndex = Math.max(0, this.data.companyList.findIndex(c => c.id === cls.companyId))
    this.setData({
      modalVisible: true,
      modalMode: 'edit',
      editClassId: cls.id,
      form: {
        companyIndex,
        week: String(cls.week || ''),
        startSession: String(cls.startSession || ''),
        endSession: String(cls.endSession || ''),
        status: cls.status || 1
      }
    })
  },

  // ===== 删除班级 =====
  onDeleteClass() {
    if (!this.data.isSuperAdmin) {
      wx.showToast({ title: '仅超级管理员可删除班级', icon: 'none' })
      return
    }
    const cls = this.data.classList[this.data.classIndex]
    if (!cls) return
    wx.showModal({
      title: '确认删除',
      content: `删除班级「${cls.className}」？班级下有学生时无法删除。`,
      success: r => {
        if (!r.confirm) return
        wx.showLoading({ title: '删除中...', mask: true })
        api.deleteClass(cls.id).then(() => {
          wx.hideLoading()
          wx.showToast({ title: '已删除', icon: 'success' })
          this.loadClasses()
        }).catch(err => {
          wx.hideLoading()
          wx.showToast({ title: (err && err.message) || '删除失败', icon: 'none' })
        })
      }
    })
  },

  // ===== 表单输入 =====
  onFormCompanyChange(e) { this.setData({ 'form.companyIndex': e.detail.value }) },
  onFormWeek(e) { this.setData({ 'form.week': e.detail.value }) },
  onFormStart(e) { this.setData({ 'form.startSession': e.detail.value }) },
  onFormEnd(e) { this.setData({ 'form.endSession': e.detail.value }) },
  onFormStatusChange(e) { this.setData({ 'form.status': Number(e.detail.value) === 0 ? 1 : 0 }) },

  // ===== 确认提交 =====
  confirmForm() {
    const { modalMode, editClassId, form, companyList } = this.data
    const week = parseInt(form.week, 10)
    const startSession = parseInt(form.startSession, 10)
    const endSession = parseInt(form.endSession, 10)
    if (!week || !startSession || !endSession) {
      wx.showToast({ title: '周次和节次必须为正整数', icon: 'none' })
      return
    }
    if (modalMode === 'add' && this.data.isSuperAdmin && !companyList[form.companyIndex]) {
      wx.showToast({ title: '请选择公司', icon: 'none' })
      return
    }

    wx.showLoading({ title: '保存中...', mask: true })
    if (modalMode === 'add') {
      const body = { week, startSession, endSession }
      if (this.data.isSuperAdmin && companyList[form.companyIndex]) {
        body.companyId = companyList[form.companyIndex].id
      }
      api.createClass(body).then(() => {
        wx.hideLoading()
        this.setData({ modalVisible: false })
        wx.showToast({ title: '已新增', icon: 'success' })
        this.loadClasses()
      }).catch(err => {
        wx.hideLoading()
        wx.showToast({ title: (err && err.message) || '创建失败', icon: 'none' })
      })
    } else {
      api.updateClass(editClassId, { week, startSession, endSession, status: form.status }).then(() => {
        wx.hideLoading()
        this.setData({ modalVisible: false })
        wx.showToast({ title: '已修改', icon: 'success' })
        this.loadClasses()
      }).catch(err => {
        wx.hideLoading()
        wx.showToast({ title: (err && err.message) || '修改失败', icon: 'none' })
      })
    }
  },

  hideModal() {
    this.setData({ modalVisible: false })
  },

  onBack() {
    const pages = getCurrentPages()
    if (pages.length > 1) wx.navigateBack()
    else wx.reLaunch({ url: '/pages/teacher/teacher' })
  }
})
