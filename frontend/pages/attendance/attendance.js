const { students: mockStudents, modifyApplications: mockApps } = require('../../utils/mockData.js')

const STORAGE_KEY = 'attendanceData'

Page({
  data: {
    className: '',
    students: [],
    keyboardVisible: false,
    activeIdx: -1,
    bufferScore: '',
    keys: ['1', '2', '3', '4', '5', '6', '7', '8', '9', 'J', 'K', 'del', 'confirm'],
    focusStudentId: ''
  },

  onLoad(options) {
    const className = decodeURIComponent(options.className || '班级')
    const focusStudentId = options.focusStudentId || ''
    const autoOpen = options.autoOpen

    this.setData({ className, focusStudentId })

    // 首次进入：若无本地数据则基于 mock 构建并写入
    this.initStorageIfNeeded()

    if (focusStudentId) {
      setTimeout(() => this.locateStudent(focusStudentId), 300)
      // 从修改申请页「去修改考勤」跳入且老师已同意：自动弹键盘改分
      if (autoOpen === '1') {
        setTimeout(() => {
          const idx = this.data.students.findIndex(s => s.id === focusStudentId)
          if (idx >= 0 && this.data.students[idx].btnState === 'approved') {
            this.setData({
              keyboardVisible: true,
              activeIdx: idx,
              bufferScore: this.data.students[idx].inputScore || ''
            })
          }
        }, 500)
      }
    }
  },

  // 每次展示页面，优先从本地缓存读取最新数据（退出再进入/重启均不丢）
  onShow() {
    this.refreshFromStorage()
  },

  // 首次初始化本地数据
  initStorageIfNeeded() {
    let data = wx.getStorageSync(STORAGE_KEY)
    if (!data || !data.students || !data.students.length) {
      data = this.buildInitialData()
      wx.setStorageSync(STORAGE_KEY, data)
    }
    this.applyData(data)
  },

  // 从缓存读取并渲染
  refreshFromStorage() {
    const data = wx.getStorageSync(STORAGE_KEY)
    if (data && data.students) {
      this.applyData(data)
    }
  },

  // 基于 mock 构建初始数据结构
  // students: 学生列表（含 inputScore/needApproval/btnState/modifyTimestamp）
  // applications: 修改申请记录
  buildInitialData() {
    const studentList = mockStudents.map(s => {
      const app = mockApps.find(a => a.studentId === s.id)
      let btnState = 'normal'
      let needApproval = false
      let inputScore = s.score
      let modifyTimestamp = null
      if (app) {
        if (app.status === 'pending') {
          // 已提交未批阅 -> 显示需审批
          needApproval = true
          btnState = 'normal'
        } else if (app.status === 'approved') {
          // 老师已同意 -> 可直接改分
          btnState = 'approved'
        } else if (app.status === 'modified') {
          // 已修改 -> 回显当前分数 + 修改时间戳
          inputScore = app.currentScore || s.score
          modifyTimestamp = app.modifiedTime
        }
      }
      return {
        ...s,
        inputScore,
        highlight: false,
        needApproval,
        btnState,
        modifyTimestamp
      }
    })
    return {
      students: studentList,
      applications: mockApps.map(a => ({ ...a }))
    }
  },

  // 将本地数据应用到视图（保留当前高亮状态）
  applyData(data) {
    const current = this.data.students
    const students = data.students.map(s => {
      const old = current.find(c => c.id === s.id)
      return { ...s, highlight: old ? old.highlight : false }
    })
    this.setData({ students })
  },

  // 保存学生数据到本地
  saveStudentsToStorage(students) {
    const data = wx.getStorageSync(STORAGE_KEY) || { applications: [] }
    data.students = students
    wx.setStorageSync(STORAGE_KEY, data)
  },

  // 同步更新某学生的申请记录
  updateApplication(studentId, patch) {
    const data = wx.getStorageSync(STORAGE_KEY) || { students: [], applications: [] }
    if (!data.applications) data.applications = []
    const idx = data.applications.findIndex(a => a.studentId === studentId)
    if (idx >= 0) {
      data.applications[idx] = { ...data.applications[idx], ...patch }
    }
    wx.setStorageSync(STORAGE_KEY, data)
  },

  // 滚动定位高亮
  locateStudent(studentId) {
    const idx = this.data.students.findIndex(s => s.id === studentId)
    if (idx < 0) {
      wx.showToast({ title: '未找到该学生', icon: 'none' })
      return
    }
    const students = this.data.students.map((s, i) => ({ ...s, highlight: i === idx }))
    this.setData({ students })
    wx.pageScrollTo({ selector: `#student-${studentId}`, duration: 400, offsetTop: -20 })
    setTimeout(() => {
      const resetList = this.data.students.map(s => ({ ...s, highlight: false }))
      this.setData({ students: resetList })
    }, 3000)
  },

  // 打分框点击：空 -> 弹键盘；非空 -> 无响应（只读）
  onScoreTap(e) {
    const idx = e.currentTarget.dataset.idx
    const student = this.data.students[idx]
    if (!student.inputScore) {
      // 场景A：内容为空 -> 弹键盘
      this.setData({
        keyboardVisible: true,
        activeIdx: idx,
        bufferScore: ''
      })
    }
    // 场景B：内容非空 -> 无响应
  },

  // 键盘按键
  onKeyTap(e) {
    const key = e.currentTarget.dataset.key
    const idx = this.data.activeIdx
    if (idx < 0) return

    if (key === 'del') {
      this.setData({ bufferScore: '' })
    } else if (key === 'confirm') {
      const newScore = this.data.bufferScore
      if (!newScore) {
        wx.showToast({ title: '请先输入分数', icon: 'none' })
        return
      }

      const student = this.data.students[idx]
      const updates = {
        [`students[${idx}].inputScore`]: newScore,
        keyboardVisible: false,
        activeIdx: -1,
        bufferScore: ''
      }

      let appPatch = null
      // 老师已同意触发的修改：完成后恢复 normal，并记录 modifyTimestamp（3 天后撤销倒计时基准）
      if (student.btnState === 'approved') {
        const modifyTimestamp = Date.now()
        updates[`students[${idx}].btnState`] = 'normal'
        updates[`students[${idx}].modifyTimestamp`] = modifyTimestamp
        appPatch = {
          status: 'modified',
          statusText: '已修改',
          currentScore: newScore,
          modifiedTime: modifyTimestamp
        }
      }

      this.setData(updates)

      // 持久化：学生数据 + 申请记录
      this.saveStudentsToStorage(this.data.students)
      if (appPatch) {
        this.updateApplication(student.id, appPatch)
      }

      wx.showToast({ title: '修改成功', icon: 'success' })
    } else {
      this.setData({ bufferScore: key })
    }
  },

  // 遮罩收起
  onHideKeyboard() {
    this.setData({ keyboardVisible: false, activeIdx: -1, bufferScore: '' })
  },

  // 修改按钮：动态切换
  // normal -> 跳页面五填表
  // approved -> 弹键盘改分，改完后恢复 normal
  onGoModify(e) {
    const idx = e.currentTarget.dataset.idx
    const student = this.data.students[idx]
    const { className } = this.data

    if (student.btnState === 'approved') {
      // 状态2：老师已同意 -> 弹键盘
      this.setData({
        keyboardVisible: true,
        activeIdx: idx,
        bufferScore: student.inputScore || ''
      })
    } else {
      // 状态1：正常/待审批 -> 跳页面五填表
      wx.navigateTo({
        url: `/pages/modifyEdit/modifyEdit?studentId=${student.id}&studentName=${encodeURIComponent(student.name)}&originScore=${student.inputScore || ''}&className=${encodeURIComponent(className)}`
      })
    }
  }
})
