// 认证工具：Token 存取 + 角色判断
const TOKEN_KEY = 'authToken'
const USER_KEY = 'userInfo'

module.exports = {
  // 保存登录返回的 Token + userInfo
  saveAuth(token, userInfo) {
    wx.setStorageSync(TOKEN_KEY, token)
    wx.setStorageSync(USER_KEY, userInfo)
    const app = getApp()
    if (app && app.globalData) {
      app.globalData.userInfo = userInfo
      app.globalData.role = this.getPrimaryRole(userInfo)
    }
  },

  getToken() {
    return wx.getStorageSync(TOKEN_KEY) || ''
  },

  getUserInfo() {
    return wx.getStorageSync(USER_KEY) || null
  },

  clearAuth() {
    wx.removeStorageSync(TOKEN_KEY)
    wx.removeStorageSync(USER_KEY)
    const app = getApp()
    if (app && app.globalData) {
      app.globalData.userInfo = null
      app.globalData.role = null
    }
  },

  // 取首个角色作为主角色（兼容后端 roles 数组）
  getPrimaryRole(userInfo) {
    if (!userInfo) return null
    const roles = userInfo.roles || []
    if (!roles.length) return null
    // 优先级：SUPER_ADMIN > TEACHER > ASSISTANT
    if (roles.indexOf('SUPER_ADMIN') >= 0) return 'super_admin'
    if (roles.indexOf('TEACHER') >= 0) return 'teacher'
    if (roles.indexOf('ASSISTANT') >= 0) return 'assistant'
    // 兼容字符串
    return this.normalizeRole(roles[0])
  },

  // 后端角色编码 -> 前端 key
  normalizeRole(code) {
    if (!code) return null
    const map = {
      SUPER_ADMIN: 'super_admin',
      TEACHER: 'teacher',
      ASSISTANT: 'assistant'
    }
    return map[code] || code.toLowerCase()
  },

  hasRole(code) {
    const user = this.getUserInfo()
    if (!user || !user.roles) return false
    return user.roles.indexOf(code) >= 0
  },

  isSuperAdmin() {
    return this.hasRole('SUPER_ADMIN')
  },

  isTeacher() {
    return this.hasRole('TEACHER')
  },

  isAssistant() {
    return this.hasRole('ASSISTANT')
  },

  // 小程序端只接受单一助教身份，教师或超管（含兼任）均不可进入。
  isAssistantOnly() {
    const user = this.getUserInfo()
    const roles = (user && user.roles) || []
    return roles.indexOf('ASSISTANT') >= 0
      && roles.indexOf('SUPER_ADMIN') < 0
      && roles.indexOf('TEACHER') < 0
  }
}
