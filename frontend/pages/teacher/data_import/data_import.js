const STORAGE_KEY = 'attendanceData'

Page({
  data: {},

  // 导入学生数据
  onImportStudents() {
    wx.chooseMessageFile({
      count: 1,
      type: 'file',
      extension: ['xlsx', 'xls', 'csv'],
      success: res => {
        const file = res.tempFiles[0]
        // TODO: 此处需接入后端解析接口，当前先用 mock 数据存入 Storage
        // 后端接口示例：wx.uploadFile({ url: '/api/import/students', filePath: file.path, ... })
        this.mockImportStudents(file.name)
      },
      fail: () => {
        wx.showToast({ title: '未选择文件', icon: 'none' })
      }
    })
  },

  // 导入助教数据
  onImportAssistants() {
    wx.chooseMessageFile({
      count: 1,
      type: 'file',
      extension: ['xlsx', 'xls', 'csv'],
      success: res => {
        const file = res.tempFiles[0]
        // TODO: 此处需接入后端解析接口，当前先用 mock 数据存入 Storage
        this.mockImportAssistants(file.name)
      },
      fail: () => {
        wx.showToast({ title: '未选择文件', icon: 'none' })
      }
    })
  },

  // Mock 导入学生数据（占位实现）
  mockImportStudents(fileName) {
    // 模拟解析结果
    const mockNewStudents = [
      { id: '20230009', name: '郑伟', college: '农学院', major: '农学', score: '', className: '周一-12节-茶园' },
      { id: '20230010', name: '冯敏', college: '园艺学院', major: '园艺', score: '', className: '周一-12节-茶园' }
    ]
    const data = wx.getStorageSync(STORAGE_KEY) || { students: [], applications: [] }
    // 合并去重
    const existIds = new Set((data.students || []).map(s => s.id))
    const merged = [...(data.students || [])]
    mockNewStudents.forEach(s => { if (!existIds.has(s.id)) merged.push(s) })
    data.students = merged
    wx.setStorageSync(STORAGE_KEY, data)
    wx.showToast({ title: '导入成功', icon: 'success' })
  },

  // Mock 导入助教数据（占位实现）
  mockImportAssistants(fileName) {
    const mockNewAssistants = [
      { id: 'T2023004', name: '吴助教', score: '' }
    ]
    const data = wx.getStorageSync(STORAGE_KEY) || { students: [], assistants: [] }
    if (!data.assistants) data.assistants = []
    const existIds = new Set(data.assistants.map(a => a.id))
    mockNewAssistants.forEach(a => { if (!existIds.has(a.id)) data.assistants.push(a) })
    wx.setStorageSync(STORAGE_KEY, data)
    wx.showToast({ title: '导入成功', icon: 'success' })
  },

  onBack() {
    const pages = getCurrentPages()
    if (pages.length > 1) wx.navigateBack()
    else wx.reLaunch({ url: '/pages/teacher/teacher' })
  }
})