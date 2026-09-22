const api = require('../../../utils/api.js')

Page({
  data: {
    // 班级
    classList: [],
    classOptions: [],
    classIndex: 0,
    // 学年学期
    yearOptions: ['2024-2025', '2025-2026', '2026-2027', '2027-2028'],
    yearIndex: 2,
    semesterOptions: ['第一学期', '第二学期'],
    semesterIndex: 0,
    // 成绩列表
    scores: [],
    loading: false,
    // 登记弹窗
    editVisible: false,
    editStudent: null,
    editScore: '',
    editRemark: ''
  },

  onLoad() {
    this.loadClasses()
  },

  // 拉取班级下拉
  loadClasses() {
    api.listClasses().then(classes => {
      const list = (classes || []).map(c => ({
        id: c.id,
        className: c.className || c.classCode || ('班级#' + c.id)
      }))
      this.setData({
        classList: list,
        classOptions: list.map(c => c.className),
        classIndex: 0
      })
      if (list.length) this.loadScores()
    }).catch(err => {
      wx.showToast({ title: (err && err.message) || '加载班级失败', icon: 'none' })
    })
  },

  onClassChange(e) {
    this.setData({ classIndex: e.detail.value }, () => this.loadScores())
  },

  onYearChange(e) {
    this.setData({ yearIndex: e.detail.value }, () => this.loadScores())
  },

  onSemesterChange(e) {
    this.setData({ semesterIndex: e.detail.value }, () => this.loadScores())
  },

  currentYear() {
    return this.data.yearOptions[this.data.yearIndex]
  },

  currentSemester() {
    return this.data.semesterIndex + 1
  },

  currentClassId() {
    const c = this.data.classList[this.data.classIndex]
    return c ? c.id : null
  },

  // 拉取换证成绩
  loadScores() {
    const classId = this.currentClassId()
    if (!classId) return
    this.setData({ loading: true })
    wx.showLoading({ title: '加载中...', mask: true })
    api.listCertificateScores(classId, this.currentYear(), this.currentSemester()).then(rows => {
      const scores = (rows || []).map(r => ({
        id: r.id,
        studentId: r.studentId,
        studentNo: r.studentNo || '',
        studentName: r.studentName || '',
        studentNoInClass: r.studentNoInClass,
        finalScore: r.finalScore !== null && r.finalScore !== undefined ? Number(r.finalScore) : null,
        remark: r.remark || ''
      }))
      this.setData({ scores, loading: false })
      wx.hideLoading()
    }).catch(err => {
      wx.hideLoading()
      this.setData({ loading: false })
      wx.showToast({ title: (err && err.message) || '加载失败', icon: 'none' })
    })
  },

  // 导出 Excel
  onExport() {
    const classId = this.currentClassId()
    if (!classId) return
    wx.showLoading({ title: '导出中...', mask: true })
    api.exportCertificateScores(classId, this.currentYear(), this.currentSemester()).then(res => {
      wx.hideLoading()
      this.saveAndOpenExcel(res.data, '换证成绩.xlsx')
    }).catch(err => {
      wx.hideLoading()
      wx.showToast({ title: (err && err.message) || '导出失败', icon: 'none' })
    })
  },

  saveAndOpenExcel(buf, fileName) {
    if (!buf) { wx.showToast({ title: '文件为空', icon: 'none' }); return }
    const fs = wx.getFileSystemManager()
    const filePath = `${wx.env.USER_DATA_PATH}/${fileName}`
    fs.writeFile({
      filePath, data: buf, encoding: 'binary',
      success: () => {
        wx.showModal({
          title: '导出成功', content: '是否打开文件？',
          success: r => { if (r.confirm) wx.openDocument({ filePath, showMenu: true, fail: () => wx.showToast({ title: '请在聊天中查看', icon: 'none' }) }) }
        })
      },
      fail: () => wx.showToast({ title: '文件保存失败', icon: 'none' })
    })
  },

  // 登记成绩
  onEditScore(e) {
    const idx = e.currentTarget.dataset.idx
    const student = this.data.scores[idx]
    this.setData({
      editVisible: true,
      editStudent: student,
      editScore: student.finalScore !== null ? String(student.finalScore) : '',
      editRemark: student.remark || ''
    })
  },

  onScoreInput(e) { this.setData({ editScore: e.detail.value }) },
  onRemarkInput(e) { this.setData({ editRemark: e.detail.value }) },

  // 确认保存成绩
  confirmScore() {
    const { editStudent, editScore, editRemark } = this.data
    if (!editStudent) return
    const score = parseFloat(editScore)
    if (isNaN(score) || score < 0 || score > 100) {
      wx.showToast({ title: '成绩须在 0~100 之间', icon: 'none' })
      return
    }
    wx.showLoading({ title: '保存中...', mask: true })
    api.saveCertificateScore(editStudent.studentId, {
      classId: this.currentClassId(),
      academicYear: this.currentYear(),
      semester: this.currentSemester(),
      finalScore: score,
      remark: editRemark || ''
    }).then(() => {
      wx.hideLoading()
      this.setData({ editVisible: false, editStudent: null })
      wx.showToast({ title: '已保存', icon: 'success' })
      this.loadScores()
    }).catch(err => {
      wx.hideLoading()
      wx.showToast({ title: (err && err.message) || '保存失败', icon: 'none' })
    })
  },

  // 删除成绩
  onDeleteScore(e) {
    const idx = e.currentTarget.dataset.idx
    const student = this.data.scores[idx]
    if (!student || student.id === null || student.id === undefined) {
      wx.showToast({ title: '暂无成绩可删', icon: 'none' })
      return
    }
    wx.showModal({
      title: '确认删除',
      content: `删除 ${student.studentName} 的换证成绩？`,
      success: r => {
        if (!r.confirm) return
        wx.showLoading({ title: '删除中...', mask: true })
        api.deleteCertificateScore(
          student.studentId,
          this.currentClassId(),
          this.currentYear(),
          this.currentSemester()
        ).then(() => {
          wx.hideLoading()
          wx.showToast({ title: '已删除', icon: 'success' })
          this.loadScores()
        }).catch(err => {
          wx.hideLoading()
          wx.showToast({ title: (err && err.message) || '删除失败', icon: 'none' })
        })
      }
    })
  },

  hideEdit() {
    this.setData({ editVisible: false, editStudent: null })
  },

  onBack() {
    const pages = getCurrentPages()
    if (pages.length > 1) wx.navigateBack()
    else wx.reLaunch({ url: '/pages/teacher/teacher' })
  }
})
