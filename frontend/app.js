const { students, modifyApplications } = require('./utils/mockData.js')

const STORAGE_KEY = 'attendanceData'
const LOG_KEY = 'operationLogs'
const TEACHER_KEY = 'teacherData'
const CLASS_KEY = 'classData'

App({
  globalData: {
    userInfo: null,
    role: null, // teacher / assistant / student
    modifyBadge: 2 // 修改考勤未读数量（Mock）
  },

  onLaunch() {
    // 应用启动时初始化本地考勤数据（首次安装/清缓存后基于 mock 构建）
    let data = wx.getStorageSync(STORAGE_KEY)
    if (!data || !data.students || !data.students.length) {
      const studentList = students.map(s => {
        const app = modifyApplications.find(a => a.studentId === s.id)
        let btnState = 'normal'
        let needApproval = false
        let inputScore = s.score
        let modifyTimestamp = null
        if (app) {
          if (app.status === 'pending') {
            needApproval = true
          } else if (app.status === 'approved') {
            btnState = 'approved'
          } else if (app.status === 'modified') {
            inputScore = app.currentScore || s.score
            modifyTimestamp = app.modifiedTime
          }
        }
        return { ...s, inputScore, highlight: false, needApproval, btnState, modifyTimestamp }
      })
      wx.setStorageSync(STORAGE_KEY, {
        students: studentList,
        applications: modifyApplications.map(a => ({ ...a }))
      })
    }

    // 初始化操作日志缓存
    if (!wx.getStorageSync(LOG_KEY)) {
      wx.setStorageSync(LOG_KEY, [])
    }

    // 初始化教师数据缓存（与 userData 中教师 id 保持一致，便于班级联动）
    if (!wx.getStorageSync(TEACHER_KEY) || !wx.getStorageSync(TEACHER_KEY).length) {
      wx.setStorageSync(TEACHER_KEY, [
        { id: 'U001', name: '王教授', code: 'T001', org: '计算机学院', status: 'enabled' },
        { id: 'U002', name: '李老师', code: 'T002', org: '农学院', status: 'enabled' }
      ])
    }

    // 初始化班级数据缓存
    if (!wx.getStorageSync(CLASS_KEY) || !wx.getStorageSync(CLASS_KEY).length) {
      wx.setStorageSync(CLASS_KEY, [
        { id: 'C001', name: '周一-12节-茶园', teacher_id: '', teacher_name: '' },
        { id: 'C002', name: '周二-34节-果园', teacher_id: 'U001', teacher_name: '王教授' },
        { id: 'C003', name: '周三-56节-麦田', teacher_id: '', teacher_name: '' },
        { id: 'C004', name: '周四-78节-蔬菜大棚', teacher_id: 'U002', teacher_name: '李老师' }
      ])
    }
  },

  /**
   * 全局操作日志写入工具
   * @param {string} operator 操作人
   * @param {string} content  日志摘要
   * @param {string} type     日志类型 operation / data / system
   */
  addLog(operator, content, type) {
    const logs = wx.getStorageSync(LOG_KEY) || []
    logs.unshift({
      id: 'LOG' + Date.now(),
      operator: operator || '系统',
      content: content || '',
      type: type || 'operation',
      time: this.formatTime(new Date())
    })
    wx.setStorageSync(LOG_KEY, logs)
  },

  formatTime(date) {
    const pad = n => (n < 10 ? '0' + n : '' + n)
    return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
  }
})
