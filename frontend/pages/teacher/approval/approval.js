const STORAGE_KEY = 'attendanceData'

Page({
  data: {
    pendingList: []
  },

  onShow() {
    this.loadData()
  },

  // 从本地缓存读取待审批申请（needApproval === true 或 status === 'pending'）
  loadData() {
    const data = wx.getStorageSync(STORAGE_KEY) || { students: [], applications: [] }
    const apps = data.applications || []
    // 筛选 pending 状态（助教提交后 status 为 pending，此时 needApproval 为 true）
    const pendingList = apps.filter(a => a.status === 'pending')
    this.setData({ pendingList })
  },

  // 同意审批：更新分数、needApproval=false、写 modifyTimestamp
  onApprove(e) {
    const item = e.currentTarget.dataset.item
    wx.showModal({
      title: '同意审批',
      content: `确认同意 ${item.studentName} 的修改申请？`,
      confirmColor: '#4A6B3A',
      success: res => {
        if (!res.confirm) return

        const data = wx.getStorageSync(STORAGE_KEY) || { students: [], applications: [] }
        const now = Date.now()

        // 1) 更新申请记录
        const aIdx = (data.applications || []).findIndex(a => a.id === item.id)
        if (aIdx >= 0) {
          data.applications[aIdx].status = 'approved'
          data.applications[aIdx].statusText = '已同意'
          data.applications[aIdx].modifiedTime = now
          data.applications[aIdx].teacher = '教师审批'
        }

        // 2) 更新学生记录：needApproval=false、btnState=approved（等待助教改分）
        const sIdx = (data.students || []).findIndex(s => s.id === item.studentId)
        if (sIdx >= 0) {
          data.students[sIdx].needApproval = false
          data.students[sIdx].btnState = 'approved'
        }

        wx.setStorageSync(STORAGE_KEY, data)
        wx.showToast({ title: '审批成功', icon: 'success' })
        this.loadData()
      }
    })
  },

  // 驳回审批：needApproval=false，分数不变
  onReject(e) {
    const id = e.currentTarget.dataset.id
    wx.showModal({
      title: '驳回申请',
      content: '确认驳回该修改申请？',
      confirmColor: '#999999',
      success: res => {
        if (!res.confirm) return

        const data = wx.getStorageSync(STORAGE_KEY) || { students: [], applications: [] }

        // 1) 更新申请记录
        const aIdx = (data.applications || []).findIndex(a => a.id === id)
        if (aIdx >= 0) {
          data.applications[aIdx].status = 'rejected'
          data.applications[aIdx].statusText = '已驳回'
        }

        // 2) 学生 needApproval=false（标签消失）
        const app = data.applications[aIdx]
        if (app && data.students) {
          const sIdx = data.students.findIndex(s => s.id === app.studentId)
          if (sIdx >= 0) {
            data.students[sIdx].needApproval = false
          }
        }

        wx.setStorageSync(STORAGE_KEY, data)
        wx.showToast({ title: '已驳回', icon: 'success' })
        this.loadData()
      }
    })
  },

  onBack() {
    const pages = getCurrentPages()
    if (pages.length > 1) wx.navigateBack()
    else wx.reLaunch({ url: '/pages/teacher/teacher' })
  }
})