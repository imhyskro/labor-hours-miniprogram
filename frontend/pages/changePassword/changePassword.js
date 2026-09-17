Page({
  data: {
    newPwd: '',
    confirmPwd: ''
  },

  onNewPwdInput(e) {
    this.setData({ newPwd: e.detail.value })
  },

  onConfirmPwdInput(e) {
    this.setData({ confirmPwd: e.detail.value })
  },

  /**
   * 密码校验
   * 规则：至少 8 位，仅由字母和数字组成，且必须同时包含字母和数字
   * 正则：/^(?=.*[A-Za-z])(?=.*\d)[A-Za-z\d]{8,}$/
   */
  validatePassword(pwd) {
    if (!pwd) {
      return { valid: false, message: '请输入新密码' }
    }
    if (!/^(?=.*[A-Za-z])(?=.*\d)[A-Za-z\d]{8,}$/.test(pwd)) {
      return { valid: false, message: '必须8位且包含字母和数字' }
    }
    return { valid: true, message: '' }
  },

  // 修改密码
  onChangePwd() {
    const { newPwd, confirmPwd } = this.data

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

    // 校验通过
    wx.showToast({ title: '修改成功', icon: 'success' })

    // 停留 1.5 秒后跳转回登录页，使用新密码重新登录
    setTimeout(() => {
      wx.redirectTo({ url: '/pages/login/login?changed=1' })
    }, 1500)
  }
})
