const api = require('../../../utils/api.js')

Page({
  data: {
    companies: [],
    loading: false,
    // 弹窗
    modalVisible: false,
    modalMode: 'add', // add | rename
    editId: null,
    editName: ''
  },

  onShow() {
    this.loadCompanies()
  },

  // 拉取公司列表
  loadCompanies() {
    if (this.data.loading) return
    this.setData({ loading: true })
    wx.showLoading({ title: '加载中...', mask: true })
    api.listCompanies().then(rows => {
      const companies = (rows || []).map(c => ({
        id: c.id,
        name: c.name || '',
        sortOrder: c.sortOrder || 0,
        status: c.status,
        classCount: c.classCount || 0,
        createdAt: this.fmt(c.createdAt)
      }))
      this.setData({ companies, loading: false })
      wx.hideLoading()
    }).catch(err => {
      wx.hideLoading()
      this.setData({ loading: false })
      wx.showToast({ title: (err && err.message) || '加载失败', icon: 'none' })
    })
  },

  fmt(iso) {
    if (!iso) return ''
    const s = String(iso).replace('T', ' ')
    return s.length > 19 ? s.substring(0, 19) : s
  },

  // 新增
  onAdd() {
    this.setData({ modalVisible: true, modalMode: 'add', editId: null, editName: '' })
  },

  // 改名
  onRename(e) {
    const idx = e.currentTarget.dataset.idx
    const c = this.data.companies[idx]
    this.setData({ modalVisible: true, modalMode: 'rename', editId: c.id, editName: c.name })
  },

  onNameInput(e) { this.setData({ editName: e.detail.value }) },

  // 确认提交
  confirmModal() {
    const { modalMode, editId, editName } = this.data
    const name = (editName || '').trim()
    if (!name) {
      wx.showToast({ title: '公司名称不能为空', icon: 'none' })
      return
    }
    wx.showLoading({ title: '保存中...', mask: true })
    const op = modalMode === 'add'
      ? api.createCompany(name)
      : api.renameCompany(editId, name)
    op.then(() => {
      wx.hideLoading()
      this.setData({ modalVisible: false })
      wx.showToast({ title: modalMode === 'add' ? '已新增' : '已改名', icon: 'success' })
      this.loadCompanies()
    }).catch(err => {
      wx.hideLoading()
      wx.showToast({ title: (err && err.message) || '操作失败', icon: 'none' })
    })
  },

  // 删除
  onDelete(e) {
    const idx = e.currentTarget.dataset.idx
    const c = this.data.companies[idx]
    wx.showModal({
      title: '确认删除',
      content: `删除公司「${c.name}」？该公司下有班级时无法删除。`,
      success: r => {
        if (!r.confirm) return
        wx.showLoading({ title: '删除中...', mask: true })
        api.deleteCompany(c.id).then(() => {
          wx.hideLoading()
          wx.showToast({ title: '已删除', icon: 'success' })
          this.loadCompanies()
        }).catch(err => {
          wx.hideLoading()
          wx.showToast({ title: (err && err.message) || '删除失败', icon: 'none' })
        })
      }
    })
  },

  hideModal() {
    this.setData({ modalVisible: false })
  },

  onBack() {
    const pages = getCurrentPages()
    if (pages.length > 1) wx.navigateBack()
    else wx.reLaunch({ url: '/pages/super_admin/super_admin' })
  }
})
