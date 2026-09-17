const STORAGE_KEY = 'userData'

Page({
  data: {
    keyword: '',
    roleFilter: '',
    filteredList: []
  },

  onShow() { this.loadData() },

  // 初始化 mock 用户数据（首次加载时写入 Storage）
  ensureData() {
    let data = wx.getStorageSync(STORAGE_KEY)
    if (!data || !data.length) {
      data = [
        { id: 'U001', username: 'teacher1', realName: '王教授', role: 'teacher', roleName: '教师', status: 'enabled', createTime: '2026-09-01' },
        { id: 'U002', username: 'teacher2', realName: '李老师', role: 'teacher', roleName: '教师', status: 'enabled', createTime: '2026-09-02' },
        { id: 'U003', username: 'asst001', realName: '刘助教', role: 'assistant', roleName: '助教', status: 'enabled', createTime: '2026-09-05' },
        { id: 'U004', username: 'asst002', realName: '陈助教', role: 'assistant', roleName: '助教', status: 'disabled', createTime: '2026-09-06' }
      ]
      wx.setStorageSync(STORAGE_KEY, data)
    }
    return data
  },

  loadData() {
    const all = this.ensureData()
    const { keyword, roleFilter } = this.data
    let list = all
    if (roleFilter) list = list.filter(u => u.role === roleFilter)
    if (keyword) list = list.filter(u => u.username.includes(keyword) || u.realName.includes(keyword))
    this.setData({ filteredList: list })
  },

  onSearchInput(e) { this.setData({ keyword: e.detail.value }); this.loadData() },
  onRoleTap(e) { this.setData({ roleFilter: e.currentTarget.dataset.role }); this.loadData() },

  onToggleStatus(e) {
    const idx = e.currentTarget.dataset.index
    const all = wx.getStorageSync(STORAGE_KEY) || []
    const target = this.data.filteredList[idx]
    const aIdx = all.findIndex(u => u.id === target.id)
    if (aIdx >= 0) {
      all[aIdx].status = all[aIdx].status === 'enabled' ? 'disabled' : 'enabled'
      wx.setStorageSync(STORAGE_KEY, all)
      this.loadData()
      wx.showToast({ title: all[aIdx].status === 'enabled' ? '已启用' : '已停用', icon: 'success' })
    }
  },

  onResetPwd(e) {
    const idx = e.currentTarget.dataset.index
    const target = this.data.filteredList[idx]
    wx.showModal({
      title: '重置密码',
      content: `确认重置 ${target.realName} 的密码为 123456？`,
      confirmColor: '#E53935',
      success: res => {
        if (!res.confirm) return
        const all = wx.getStorageSync(STORAGE_KEY) || []
        const aIdx = all.findIndex(u => u.id === target.id)
        if (aIdx >= 0) {
          all[aIdx].password = '123456'
          wx.setStorageSync(STORAGE_KEY, all)
          wx.showToast({ title: '密码已重置', icon: 'success' })
        }
      }
    })
  },

  onBack() {
    const pages = getCurrentPages()
    if (pages.length > 1) wx.navigateBack()
    else wx.reLaunch({ url: '/pages/super_admin/super_admin' })
  }
})