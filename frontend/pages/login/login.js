const app = getApp()
const api = require('../../utils/api.js')
const auth = require('../../utils/auth.js')

Page({
  data: {
    roleOptions: ['请选择账号类型', '超级管理员', '教师', '助教'],
    roleIndex: 2, // 默认选中教师
    role: 'teacher', // 当前角色 key：super_admin / teacher / assistant，用于动态提示
    account: '',
    password: '',
    pwdChanged: false, // 是否已修改过密码（由改密页传入）
    submitting: false
  },

  onLoad(options) {
    // 从改密页跳回时携带 changed=1，标识用户已设置新密码
    this.setData({ pwdChanged: options.changed === '1' })
  },

  // 每次显示登录页时清空输入（从工作台返回时防止残留上次账号密码）
  onShow() {
    this.setData({ account: '', password: '' })
  },

  // 选择账号类型：同步更新 roleIndex 与 role，驱动密码提示动态切换
  onRoleChange(e) {
    const roleIndex = e.detail.value
    const roleKeys = ['', 'super_admin', 'teacher', 'assistant']
    this.setData({ roleIndex, role: roleKeys[roleIndex] })
  },

  // 输入账号
  onAccountInput(e) {
    this.setData({ account: e.detail.value })
  },

  // 输入密码
  onPasswordInput(e) {
    this.setData({ password: e.detail.value })
  },

  // 登录
  onLogin() {
    const { roleIndex, account, password, submitting } = this.data

    if (submitting) return

    // 校验账号类型
    if (roleIndex === 0) {
      wx.showToast({ title: '请选择账号类型', icon: 'none' })
      return
    }

    // 校验账号
    if (!account.trim()) {
      wx.showToast({ title: '请输入账号', icon: 'none' })
      return
    }

    // 校验密码
    if (!password) {
      wx.showToast({ title: '请输入密码', icon: 'none' })
      return
    }

    // 角色映射（仅前端跳转参考，最终权限以后端 roles 为准）
    const roleMap = {
      1: { key: 'super_admin', name: '超级管理员', path: '/pages/super_admin/super_admin' },
      2: { key: 'teacher', name: '教师', path: '/pages/teacher/teacher' },
      3: { key: 'assistant', name: '助教', path: '/pages/assistant/assistant' }
    }
    const role = roleMap[roleIndex]

    this.setData({ submitting: true })
    wx.showLoading({ title: '登录中...', mask: true })

    api.login(account.trim(), password).then(data => {
      wx.hideLoading()
      // 保存登录态
      auth.saveAuth(data.token, data.userInfo)

      wx.showToast({ title: '登录成功', icon: 'success' })

      const targetPath = data.firstLogin
        ? '/pages/changePassword/changePassword?firstLogin=1'
        : role.path

      setTimeout(() => {
        // 首次登录跳改密页用 navigateTo 保留登录态；其余 reLaunch 进入工作台
        if (data.firstLogin) {
          wx.navigateTo({ url: targetPath })
        } else {
          wx.reLaunch({ url: role.path })
        }
      }, 600)
    }).catch(err => {
      wx.hideLoading()
      this.setData({ submitting: false })
      const msg = (err && err.message) || '登录失败，请检查网络'
      wx.showModal({
        title: '登录失败',
        content: msg,
        showCancel: false,
        confirmText: '确定'
      })
    })
  }
})
