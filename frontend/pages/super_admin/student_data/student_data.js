const api = require('../../../utils/api.js')

Page({
  data: {
    rawList: [],
    list: [],
    keyword: '',
    page: 1,
    size: 200,
    total: 0,
    loading: false,
    // 班级下拉
    classList: [],
    classOptions: [],
    // 弹窗
    modalVisible: false,
    modalMode: 'add', // add | edit
    editId: null,
    form: {
      studentId: '',
      name: '',
      classId: null,
      classIndex: 0,
      studentNoInClass: '',
      originalMajor: '',
      gender: 1
    },
    genderOptions: ['男', '女']
  },

  onShow() {
    this.loadData()
    this.loadClasses()
  },

  // 拉取班级下拉
  loadClasses() {
    api.listClasses().then(classes => {
      const list = (classes || []).map(c => ({
        id: c.id,
        className: c.className || c.classCode || ('班级#' + c.id)
      }))
      this.setData({ classList: list, classOptions: list.map(c => c.className) })
    }).catch(err => {
      wx.showToast({ title: '加载班级列表失败', icon: 'none' })
    })
  },

  loadData() {
    if (this.data.loading) return
    this.setData({ loading: true })
    wx.showLoading({ title: '加载中...', mask: true })

    api.masterList({
      page: this.data.page,
      size: this.data.size
    }).then(pageData => {
      const rawList = (pageData.records || []).map(r => ({
        id: r.id,
        name: r.name || '',
        code: r.studentId || '',
        college: r.companyName || '',
        major: r.originalMajor || '',
        className: r.className || '',
        identity: r.identity || '',
        classId: r.classId,
        gender: r.gender || 0,
        studentNoInClass: r.studentNoInClass || ''
      }))
      this.setData({ rawList, total: pageData.total || 0, loading: false })
      this.applyFilter()
      wx.hideLoading()
    }).catch(err => {
      wx.hideLoading()
      this.setData({ loading: false })
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

  // ===== 新增 =====
  onAdd() {
    this.setData({
      modalVisible: true,
      modalMode: 'add',
      editId: null,
      form: {
        studentId: '',
        name: '',
        classId: this.data.classList.length ? this.data.classList[0].id : null,
        classIndex: 0,
        studentNoInClass: '',
        originalMajor: '',
        gender: 1
      }
    })
  },

  // ===== 编辑 =====
  onEdit(e) {
    const idx = e.currentTarget.dataset.idx
    const s = this.data.list[idx]
    if (!s) return
    const classIndex = Math.max(0, this.data.classList.findIndex(c => c.id === s.classId))
    this.setData({
      modalVisible: true,
      modalMode: 'edit',
      editId: s.id,
      form: {
        studentId: s.code,
        name: s.name,
        classId: s.classId,
        classIndex,
        studentNoInClass: String(s.studentNoInClass || ''),
        originalMajor: s.major,
        gender: s.gender || 0
      }
    })
  },

  // ===== 删除 =====
  onDelete(e) {
    const idx = e.currentTarget.dataset.idx
    const s = this.data.list[idx]
    if (!s) return
    wx.showModal({
      title: '确认删除',
      content: `删除学生「${s.name}（${s.code}）」？删除助教学生时会同时清理其助教班级关联和登录账号。`,
      success: r => {
        if (!r.confirm) return
        wx.showLoading({ title: '删除中...', mask: true })
        api.deleteStudent(s.id).then(() => {
          wx.hideLoading()
          wx.showToast({ title: '已删除', icon: 'success' })
          this.loadData()
        }).catch(err => {
          wx.hideLoading()
          wx.showToast({ title: (err && err.message) || '删除失败', icon: 'none' })
        })
      }
    })
  },

  // ===== 表单输入 =====
  onFormStudentId(e) { this.setData({ 'form.studentId': e.detail.value }) },
  onFormName(e) { this.setData({ 'form.name': e.detail.value }) },
  onFormNoInClass(e) { this.setData({ 'form.studentNoInClass': e.detail.value }) },
  onFormMajor(e) { this.setData({ 'form.originalMajor': e.detail.value }) },
  onFormClassChange(e) {
    const idx = e.detail.value
    const c = this.data.classList[idx]
    this.setData({ 'form.classIndex': idx, 'form.classId': c ? c.id : null })
  },
  onFormGenderChange(e) {
    this.setData({ 'form.gender': Number(e.detail.value) === 0 ? 1 : 2 })
  },

  // ===== 确认提交 =====
  confirmForm() {
    const { modalMode, editId, form } = this.data
    const studentId = (form.studentId || '').trim()
    const name = (form.name || '').trim()
    if (!studentId) { wx.showToast({ title: '学号不能为空', icon: 'none' }); return }
    if (!name) { wx.showToast({ title: '姓名不能为空', icon: 'none' }); return }
    if (!form.classId) { wx.showToast({ title: '请选择班级', icon: 'none' }); return }

    const body = {
      studentId,
      name,
      classId: form.classId,
      studentNoInClass: parseInt(form.studentNoInClass, 10) || 1,
      originalMajor: form.originalMajor || '',
      gender: form.gender
    }

    wx.showLoading({ title: '保存中...', mask: true })
    const op = modalMode === 'add'
      ? api.createStudent(body)
      : api.updateStudent(editId, {
          name: body.name,
          classId: body.classId,
          studentNoInClass: body.studentNoInClass,
          originalMajor: body.originalMajor,
          gender: body.gender,
          status: 1
        })
    op.then(() => {
      wx.hideLoading()
      this.setData({ modalVisible: false })
      wx.showToast({ title: modalMode === 'add' ? '已新增' : '已修改', icon: 'success' })
      this.loadData()
    }).catch(err => {
      wx.hideLoading()
      wx.showToast({ title: (err && err.message) || '保存失败', icon: 'none' })
    })
  },

  hideModal() {
    this.setData({ modalVisible: false })
  },

  // 导入学生数据
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
    api.importStudents(filePath).then(data => {
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
          content: msg + '\n' + errs.map(e => `行${e.row} ${e.studentId}：${e.reason}`).join('\n'),
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

  // 导出学生 Excel
  onExport() {
    wx.showLoading({ title: '导出中...', mask: true })
    api.exportStudents(this.data.keyword).then(res => {
      wx.hideLoading()
      this.saveAndOpenExcel(res.data, '学生数据.xlsx')
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

  onBack() {
    const pages = getCurrentPages()
    if (pages.length > 1) wx.navigateBack()
    else wx.reLaunch({ url: '/pages/super_admin/super_admin' })
  }
})
