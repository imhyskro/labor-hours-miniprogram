const app = getApp()
const api = require('../../utils/api.js')
const auth = require('../../utils/auth.js')

Page({
  data: {
    account: '',
    password: '',
    role: 'assistant',
    roleIndex: 0,
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
      0: { key: 'assistant', name: '助教', path: '/pages/assistant/assistant' }
    }
    const role = roleMap[roleIndex]

    this.setData({ submitting: true })
    wx.showLoading({ title: '登录中...', mask: true })

    api.login(account.trim(), password).then(data => {
      wx.hideLoading()
      const roles = (data.userInfo && data.userInfo.roles) || []
      const assistantOnly = roles.indexOf('ASSISTANT') >= 0
        && roles.indexOf('SUPER_ADMIN') < 0
        && roles.indexOf('TEACHER') < 0
      if (!assistantOnly) {
        auth.clearAuth()
        throw new Error('微信小程序仅允许助教账号登录')
      }
      // 保存登录态
      auth.saveAuth(data.token, data.userInfo)

      // 不显示 Toast：避免与待办提醒 Modal 互斥导致回调异常
      const targetPath = data.firstLogin
        ? '/pages/changePassword/changePassword?firstLogin=1'
        : role.path

      setTimeout(() => {
        // 首次登录跳改密页用 navigateTo 保留登录态；其余 reLaunch 进入工作台
        if (data.firstLogin) {
          wx.navigateTo({ url: targetPath })
        } else {
          wx.reLaunch({
            url: role.path,
            success: () => {
              // 延迟弹窗，确保工作台页面已完全加载后再调用
              setTimeout(() => this.checkPendingScore(), 300)
            }
          })
        }
      }, 600)
    }).catch(err => {
      wx.hideLoading()
      this.setData({ submitting: false })
      const msg = (err && err.message) || '登录失败'
      wx.showToast({ title: msg, icon: 'none' })
    })
  },

  // 登录后检查待打分：有则弹窗提醒，无则静默放行
  checkPendingScore() {
    api.checkTodayScore().then(res => {
      const body = res && res.data
      if (body && body.code === 201) {
        wx.showModal({
          title: '⚠️ 待办提醒',
          content: body.message || '检测到您有有效期内尚未打分的课程，请尽快处理！',
          confirmText: '去打分',
          cancelText: '稍后',
          confirmColor: '#4A6B3A',
          success: r => {
              if (r.confirm) {
              // 跳转到班级管理列表页（助教工作台），用户可在班级卡片看到红点提醒
              wx.reLaunch({ url: '/pages/assistant/assistant?from=remind' })
            }
          }
        })
      }
    }).catch(() => {
      // 接口失败/超时，不阻塞用户
    })
  }
})
