Page({
  data: {},

  onShow() {},

  onGoStudentData() {
    wx.navigateTo({ url: '/pages/super_admin/student_data/student_data' })
  },

  onGoTeacherData() {
    wx.navigateTo({ url: '/pages/super_admin/teacher_data/teacher_data' })
  },

  onBack() {
    const pages = getCurrentPages()
    if (pages.length > 1) wx.navigateBack()
    else wx.reLaunch({ url: '/pages/super_admin/super_admin' })
  }
})