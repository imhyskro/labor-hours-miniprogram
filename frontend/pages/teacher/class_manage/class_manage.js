const STORAGE_KEY = 'attendanceData'

Page({
  data: {
    classOptions: [],
    classIndex: 0,
    filteredList: []
  },

  onShow() {
    this.loadData()
  },

  loadData() {
    const data = wx.getStorageSync(STORAGE_KEY) || { students: [] }
    const students = data.students || []
    const classSet = new Set()
    students.forEach(s => { if (s.className) classSet.add(s.className) })
    const classOptions = Array.from(classSet)

    if (classOptions.length === 0) {
      this.setData({ classOptions: [], filteredList: [] })
      return
    }

    const idx = Math.min(this.data.classIndex, classOptions.length - 1)
    const selectedClass = classOptions[idx]
    const filteredList = students.filter(s => s.className === selectedClass)

    this.setData({ classOptions, classIndex: idx, filteredList })
  },

  onClassChange(e) {
    this.setData({ classIndex: e.detail.value })
    this.loadData()
  },

  onAddClass() {
    wx.showModal({
      title: '新增班级',
      editable: true,
      placeholderText: '请输入班级名称，如：周三-34节-果园',
      success: res => {
        if (res.confirm && res.content && res.content.trim()) {
          const name = res.content.trim()
          const data = wx.getStorageSync(STORAGE_KEY) || { students: [], applications: [] }
          wx.setStorageSync(STORAGE_KEY, data)
          wx.showToast({ title: '班级已添加', icon: 'success' })
          this.loadData()
        }
      }
    })
  },

  onBack() {
    const pages = getCurrentPages()
    if (pages.length > 1) wx.navigateBack()
    else wx.reLaunch({ url: '/pages/teacher/teacher' })
  }
})