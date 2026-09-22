const { teachers } = require('../../utils/mockData.js')

Page({
  data: {
    studentId: '',
    studentName: '',
    originScore: '',
    className: '',
    teacherList: teachers,
    teacherIndex: 0,
    applyReason: '',
    images: [],
    maxImages: 3
  },

  onLoad(options) {
    this.setData({
      studentId: options.studentId || '',
      studentName: decodeURIComponent(options.studentName || ''),
      originScore: options.originScore || '',
      className: decodeURIComponent(options.className || '')
    })
  },

  onTeacherChange(e) {
    this.setData({ teacherIndex: e.detail.value })
  },

  onReasonInput(e) {
    this.setData({ applyReason: e.detail.value })
  },

  // 选择图片（相册或拍照）
  onChooseImage() {
    const { images, maxImages } = this.data
    if (images.length >= maxImages) {
      wx.showToast({ title: `最多上传 ${maxImages} 张`, icon: 'none' })
      return
    }
    wx.chooseImage({
      count: maxImages - images.length,
      sourceType: ['album', 'camera'],
      success: res => {
        this.setData({
          images: [...images, ...res.tempFilePaths]
        })
      }
    })
  },

  // 预览图片
  onPreviewImage(e) {
    const url = e.currentTarget.dataset.url
    wx.previewImage({
      current: url,
      urls: this.data.images
    })
  },

  // 删除图片
  onDeleteImage(e) {
    const idx = e.currentTarget.dataset.idx
    const images = this.data.images.filter((_, i) => i !== idx)
    this.setData({ images })
  },

  // 提交申请
  onSubmit() {
    const { studentId, studentName, originScore, className, teacherList, teacherIndex, applyReason, images } = this.data

    // 仅校验学生信息是否存在
    if (!studentId) {
      wx.showToast({ title: '缺少学生信息', icon: 'none' })
      return
    }
    // 申请理由、假条均为选填项，均可直接提交

    wx.showModal({
      title: '确认提交',
      content: `将申请发送给 ${teacherList[teacherIndex]}？`,
      confirmColor: '#4A6B3A',
      success: res => {
        if (!res.confirm) return

        // ===== 持久化：写入本地 attendanceData =====
        const STORAGE_KEY = 'attendanceData'
        const data = wx.getStorageSync(STORAGE_KEY) || { students: [], applications: [] }
        if (!data.students) data.students = []
        if (!data.applications) data.applications = []

        // 1) 学生 needApproval 置 true（考勤页名字旁实时显示「需审批」）
        const sIdx = data.students.findIndex(s => s.id === studentId)
        let studentInfo = null
        if (sIdx >= 0) {
          data.students[sIdx].needApproval = true
          studentInfo = data.students[sIdx]
        }

        // 2) 构建新的申请记录
        const now = new Date()
        const pad = n => String(n).padStart(2, '0')
        const applyTime = `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())} ${pad(now.getHours())}:${pad(now.getMinutes())}`
        const newApp = {
          id: 'APP_' + Date.now(),
          studentId,
          studentName,
          college: (studentInfo && studentInfo.college) || '',
          major: (studentInfo && studentInfo.major) || '',
          originScore: originScore || '',
          currentScore: originScore || '',
          applyReason: applyReason || '',
          images: images || [],
          status: 'pending',
          statusText: '待审核',
          applyTime,
          modifiedTime: null,
          teacher: teacherList[teacherIndex]
        }

        // 若该学生已有申请记录则覆盖，否则新增
        const aIdx = data.applications.findIndex(a => a.studentId === studentId)
        if (aIdx >= 0) {
          data.applications[aIdx] = newApp
        } else {
          data.applications.unshift(newApp)
        }

        // 3) 同步写入本地
        wx.setStorageSync(STORAGE_KEY, data)

        wx.showToast({ title: '申请已提交', icon: 'success' })
        setTimeout(() => {
          wx.navigateBack()
        }, 1000)
      }
    })
  }
})
