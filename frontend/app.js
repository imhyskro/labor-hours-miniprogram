const auth = require('./utils/auth.js')

App({
  globalData: {
    userInfo: null,
    role: null, // super_admin / teacher / assistant
    modifyBadge: 0 // 修改考勤未读数量（前端暂用，无后端接口）
  },

  onLaunch() {
    // 启动时从本地恢复登录态
    const userInfo = auth.getUserInfo()
    if (userInfo) {
      this.globalData.userInfo = userInfo
      this.globalData.role = auth.getPrimaryRole(userInfo)
    }
  },

  // 操作日志已由后端在各 Controller 内部自动记录，前端此方法保留为空实现以兼容旧调用
  addLog() {},

  formatTime(date) {
    const pad = n => (n < 10 ? '0' + n : '' + n)
    return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
  }
})
