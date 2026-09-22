const api = require('../../../utils/api.js')
const auth = require('../../../utils/auth.js')

Page({
  data: {
    keyword: '',
    roleFilter: '',     // '' / SUPER_ADMIN / TEACHER / ASSISTANT
    filteredList: [],
    page: 1,
    size: 50,
    total: 0,
    loading: false,
    // 用户详情弹窗
    detailVisible: false,
    detailUser: null
  },

  onShow() { this.loadData() },

  loadData() {
    if (this.data.loading) return
    this.setData({ loading: true })
    wx.showLoading({ title: '加载中...', mask: true })

    const params = {
      page: this.data.page,
      size: this.data.size,
      keyword: this.data.keyword
    }
    if (this.data.roleFilter) params.status = 1

    api.pageUsers(params).then(pageData => {
      // 后端 records 字段：id、username、realName、roles[]、status、firstLogin、lastPasswordChangeTime、createdAt
      const list = (pageData.records || []).map(u => {
        const role = (u.roles && u.roles[0]) || ''
        const frontRole = auth.normalizeRole(role)
        return {
          ...u,
          role: frontRole,
          roleName: this.roleName(role),
          // 后端 status 1/0 -> 前端字符串 'enabled'/'disabled'（与 WXML 一致）
          status: u.status === 1 ? 'enabled' : 'disabled',
          statusText: u.status === 1 ? '启用' : '停用',
          createTime: u.createdAt || ''
        }
      })
      // 前端再按角色过滤（API 无 role 过滤参数）
      const filtered = this.data.roleFilter
        ? list.filter(u => u.roles && u.roles.indexOf(this.data.roleFilter) >= 0)
        : list

      this.setData({
        filteredList: filtered,
        total: pageData.total || 0,
        loading: false
      })
      wx.hideLoading()
    }).catch(err => {
      wx.hideLoading()
      this.setData({ loading: false })
      wx.showToast({ title: (err && err.message) || '加载失败', icon: 'none' })
    })
  },

  roleName(code) {
    const map = { SUPER_ADMIN: '超管', TEACHER: '教师', ASSISTANT: '助教' }
    return map[code] || code || ''
  },

  onSearchInput(e) { this.setData({ keyword: e.detail.value, page: 1 }); this.loadData() },
  onRoleTap(e) { this.setData({ roleFilter: e.currentTarget.dataset.role, page: 1 }); this.loadData() },

  onToggleStatus(e) {
    const idx = e.currentTarget.dataset.index
    const target = this.data.filteredList[idx]
    if (!target) return
    // 防止停用自己的账号
    const me = auth.getUserInfo()
    if (me && me.id === target.id) {
      wx.showToast({ title: '不能停用自己的账号', icon: 'none' })
      return
    }
    const next = target.status === 'enabled' ? 0 : 1
    wx.showModal({
      title: '确认',
      content: `确认${next === 1 ? '启用' : '停用'} ${target.realName} 的账号？`,
      confirmColor: '#4A6B3A',
      success: res => {
        if (!res.confirm) return
        wx.showLoading({ title: '处理中...', mask: true })
        api.setUserStatus(target.id, next).then(() => {
          wx.hideLoading()
          wx.showToast({ title: next === 1 ? '已启用' : '已停用', icon: 'success' })
          this.loadData()
        }).catch(err => {
          wx.hideLoading()
          wx.showToast({ title: (err && err.message) || '操作失败', icon: 'none' })
        })
      }
    })
  },

  onResetPwd(e) {
    const idx = e.currentTarget.dataset.index
    const target = this.data.filteredList[idx]
    if (!target) return
    const me = auth.getUserInfo()
    if (me && me.id === target.id) {
      wx.showToast({ title: '不能重置自己的账号', icon: 'none' })
      return
    }
    wx.showModal({
      title: '重置密码',
      content: `确认重置 ${target.realName} 的密码？重置后用户下次登录需修改密码。`,
      confirmColor: '#E53935',
      success: res => {
        if (!res.confirm) return
        wx.showLoading({ title: '处理中...', mask: true })
        api.resetUserPassword(target.id).then(() => {
          wx.hideLoading()
          wx.showToast({ title: '密码已重置', icon: 'success' })
          this.loadData()
        }).catch(err => {
          wx.hideLoading()
          wx.showToast({ title: (err && err.message) || '重置失败', icon: 'none' })
        })
      }
    })
  },

  onCreateTeacher() {
    wx.showModal({
      title: '新建教师账号',
      editable: true,
      placeholderText: '请输入登录用户名',
      success: r => {
        if (!r.confirm || !r.content || !r.content.trim()) return
        const username = r.content.trim()
        wx.showModal({
          title: '请输入真实姓名',
          editable: true,
          placeholderText: '如：张老师',
          success: r2 => {
            if (!r2.confirm || !r2.content || !r2.content.trim()) return
            const realName = r2.content.trim()
            wx.showLoading({ title: '创建中...', mask: true })
            api.createTeacher(username, realName).then(() => {
              wx.hideLoading()
              wx.showToast({ title: '创建成功', icon: 'success' })
              this.loadData()
            }).catch(err => {
              wx.hideLoading()
              wx.showToast({ title: (err && err.message) || '创建失败', icon: 'none' })
            })
          }
        })
      }
    })
  },

  // 查看用户详情
  onViewDetail(e) {
    const idx = e.currentTarget.dataset.index
    const target = this.data.filteredList[idx]
    if (!target) return
    wx.showLoading({ title: '加载详情...', mask: true })
    api.getUser(target.id).then(u => {
      wx.hideLoading()
      this.setData({
        detailVisible: true,
        detailUser: {
          id: u.id,
          username: u.username || '',
          realName: u.realName || '',
          roles: (u.roles || []).join('、') || '',
          status: u.status === 1 ? '启用' : '停用',
          firstLogin: u.firstLogin ? '待完成' : '已完成',
          lastPasswordChangeTime: this.fmt(u.lastPasswordChangeTime),
          createdAt: this.fmt(u.createdAt)
        }
      })
    }).catch(err => {
      wx.hideLoading()
      wx.showToast({ title: (err && err.message) || '加载失败', icon: 'none' })
    })
  },

  fmt(iso) {
    if (!iso) return '—'
    const s = String(iso).replace('T', ' ')
    return s.length > 19 ? s.substring(0, 19) : s
  },

  hideDetail() {
    this.setData({ detailVisible: false, detailUser: null })
  },

  onBack() {
    const pages = getCurrentPages()
    if (pages.length > 1) wx.navigateBack()
    else wx.reLaunch({ url: '/pages/super_admin/super_admin' })
  }
})
