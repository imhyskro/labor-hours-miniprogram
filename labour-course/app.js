const auth = require('./utils/auth.js')

App({
  globalData: {
    userInfo: null,
    role: null, // super_admin / teacher / assistant
    modifyBadge: 0, // 修改考勤未读数量（前端暂用，无后端接口）
    classListDirty: false // 考勤页打分后置 true，班级页 onShow 重新检查红点
  },

  onLaunch() {
    // 启动时从本地恢复登录态
    const userInfo = auth.getUserInfo()
    if (userInfo && auth.isAssistantOnly()) {
      this.globalData.userInfo = userInfo
      this.globalData.role = auth.getPrimaryRole(userInfo)
    } else if (userInfo) {
      auth.clearAuth()
    }
  },

  // 操作日志已由后端在各 Controller 内部自动记录，前端此方法保留为空实现以兼容旧调用
  addLog() {},

  formatTime(date) {
    const pad = n => (n < 10 ? '0' + n : '' + n)
    return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
  }
})
