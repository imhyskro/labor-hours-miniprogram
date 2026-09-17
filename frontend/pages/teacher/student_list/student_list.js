const STORAGE_KEY = 'attendanceData'

Page({
  data: {
    classOptions: ['全部班级'],
    classIndex: 0,
    filteredList: []
  },

  onShow() {
    this.loadData()
  },

  loadData() {
    const data = wx.getStorageSync(STORAGE_KEY) || { students: [] }
    const students = data.students || []

    // 从本地缓存中提取所有唯一班级
    const classSet = new Set()
    students.forEach(s => { if (s.className) classSet.add(s.className) })
    const classOptions = ['全部班级', ...Array.from(classSet)]

    let filteredList = students
    if (this.data.classIndex > 0) {
      const selectedClass = classOptions[this.data.classIndex]
      filteredList = students.filter(s => s.className === selectedClass)
    }

    this.setData({ classOptions, filteredList })
  },

  onClassChange(e) {
    this.setData({ classIndex: e.detail.value })
    this.loadData()
  },

  onBack() {
    const pages = getCurrentPages()
    if (pages.length > 1) wx.navigateBack()
    else wx.reLaunch({ url: '/pages/teacher/teacher' })
  }
})