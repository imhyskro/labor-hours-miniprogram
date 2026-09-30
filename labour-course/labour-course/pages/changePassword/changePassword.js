const api = require('../../utils/api.js')
const auth = require('../../utils/auth.js')

Page({
  data: {
    oldPwd: '',
    newPwd: '',
    confirmPwd: '',
    firstLogin: false,
    submitting: false
  },

  onLoad(options) {
    this.setData({ firstLogin: options.firstLogin === '1' })
  },

  onOldPwdInput(e) {
    this.setData({ oldPwd: e.detail.value })
  },

  onNewPwdInput(e) {
    this.setData({ newPwd: e.detail.value })
  },

  onConfirmPwdInput(e) {
    this.setData({ confirmPwd: e.detail.value })
  },

  /**
   * 新密码校验（依据 API 文档）
   * 至少 8 位，且在「大写字母、小写字母、数字、特殊字符 @$!%*?&」四类中至少包含三类
   */
  validatePassword(pwd) {
    if (!pwd) {
      return { valid: false, message: '请输入新密码' }
    }
    if (pwd.length < 8) {
      return { valid: false, message: '密码至少 8 位' }
    }
    let classes = 0
    if (/[a-z]/.test(pwd)) classes++
    if (/[A-Z]/.test(pwd)) classes++
    if (/\d/.test(pwd)) classes++
    if (/[@$!%*?&]/.test(pwd)) classes++
    if (classes < 3) {
      return { valid: false, message: '密码需包含大写、小写、数字、特殊字符(@$!%*?&)中至少三类' }
    }
    return { valid: true, message: '' }
  },

  // 修改密码
  onChangePwd() {
    const { oldPwd, newPwd, confirmPwd, submitting, firstLogin } = this.data
    if (submitting) return

    // 首次登录场景下后端要求 oldPassword（即初始密码），仍需填写
    if (!oldPwd) {
      wx.showToast({ title: '请输入原密码', icon: 'none' })
      return
    }

    // 校验新密码格式
    const check = this.validatePassword(newPwd)
    if (!check.valid) {
      wx.showToast({ title: check.message, icon: 'none' })
      return
    }

    // 校验两次密码一致
    if (newPwd !== confirmPwd) {
      wx.showToast({ title: '两次输入的密码不一致', icon: 'none' })
      return
    }

    this.setData({ submitting: true })
    wx.showLoading({ title: '提交中...', mask: true })

    api.changePassword(oldPwd, newPwd, confirmPwd).then(() => {
      wx.hideLoading()
      // 改密成功后清除旧 Token，要求用户重新登录
      auth.clearAuth()
      wx.showToast({ title: '密码修改成功', icon: 'success' })
      setTimeout(() => {
        wx.redirectTo({ url: '/pages/login/login' })
      }, 1200)
    }).catch(err => {
      wx.hideLoading()
      this.setData({ submitting: false })
      const msg = (err && err.message) || '修改失败'
      wx.showToast({ title: msg, icon: 'none' })
    })
  }
})
