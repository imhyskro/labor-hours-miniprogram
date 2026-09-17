const app = getApp()

Page({
  data: {
    roleOptions: ['请选择账号类型', '超级管理员', '教师', '助教'],
    roleIndex: 2, // 默认选中教师
    role: 'teacher', // 当前角色 key：super_admin / teacher / assistant，用于动态提示
    account: '',
    password: '',
    pwdChanged: false // 是否已修改过密码（由改密页传入）
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

  /**
   * 密码校验（正常登录/已改密后使用）
   * 规则：8 位及以上，仅由字母和数字组成，且必须同时包含字母和数字
   * 正则：/^(?=.*[A-Za-z])(?=.*\d)[A-Za-z\d]{8,}$/
   */
  validatePassword(pwd) {
    if (!pwd) {
      return { valid: false, message: '请输入密码' }
    }
    if (!/^(?=.*[A-Za-z])(?=.*\d)[A-Za-z\d]{8,}$/.test(pwd)) {
      return { valid: false, message: '密码必须8位且包含字母和数字' }
    }
    return { valid: true, message: '' }
  },

  // 登录
  onLogin() {
    const { roleIndex, account, password } = this.data

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

    // 角色映射（索引与 roleKeys 对齐：1=超级管理员 2=教师 3=助教）
    const roleMap = {
      1: { key: 'super_admin', name: '超级管理员', path: '/pages/super_admin/super_admin' },
      2: { key: 'teacher', name: '教师', path: '/pages/teacher/teacher' },
      3: { key: 'assistant', name: '助教', path: '/pages/assistant/assistant' }
    }
    const role = roleMap[roleIndex]

    // 助教：区分「首次登录」与「正常登录」
    // - 9 位纯数字 => 初始密码（学号）=> 首次登录 => 强制跳转改密页
    // - 其他格式  => 已改密 => 走正常登录校验
    // 使用 role.key 判断而非 roleIndex，避免角色顺序变化导致索引错位
    if (role.key === 'assistant' && /^\d{9}$/.test(password)) {
      wx.redirectTo({ url: '/pages/changePassword/changePassword' })
      return
    }

    // 正常登录：密码格式校验（8 位及以上，含字母和数字）
    const pwdCheck = this.validatePassword(password)
    if (!pwdCheck.valid) {
      wx.showToast({ title: pwdCheck.message, icon: 'none' })
      return
    }

    // 写入登录态（Mock）
    const userInfo = {
      account: account.trim(),
      role: role.key,
      roleName: role.name
    }
    wx.setStorageSync('userInfo', userInfo)
    app.globalData.userInfo = userInfo
    app.globalData.role = role.key

    wx.showToast({ title: '登录成功', icon: 'success' })

    // 跳转对应工作台（使用 navigateTo 保留 login 在页面栈，便于返回）
    setTimeout(() => {
      wx.navigateTo({ url: role.path })
    }, 600)
  }
})
