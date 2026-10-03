// 助教端共享 Mock 数据

// 学生列表（用于考勤记录详情页）
const students = [
  { id: '20230001', name: '张明', college: '农学院', major: '农学', score: '' },
  { id: '20230002', name: '李华', college: '农学院', major: '园艺', score: '' },
  { id: '20230003', name: '王芳', college: '植物保护学院', major: '植保', score: '' },
  { id: '20230004', name: '赵磊', college: '农学院', major: '农学', score: '' },
  { id: '20230005', name: '钱秀英', college: '园艺学院', major: '园艺', score: '' },
  { id: '20230006', name: '孙强', college: '农学院', major: '农学', score: '' },
  { id: '20230007', name: '周丽', college: '植物保护学院', major: '植保', score: '' },
  { id: '20230008', name: '吴伟', college: '园艺学院', major: '园艺', score: '' }
]

// 修改申请记录（用于修改申请状态页）
// status: pending / approved / modified / rejected
// modifiedTime: 助教完成修改的时间戳（ms），3天自动撤销从这里开始计算
const modifyApplications = [
  {
    id: 'APP20260901001',
    studentId: '20230003',
    studentName: '王芳',
    college: '植物保护学院',
    major: '植保',
    originScore: 'K',
    currentScore: 'K',
    applyReason: '当天身体不适提前请假，缺勤记录有误',
    status: 'pending',
    statusText: '待审核',
    applyTime: '2026-09-08 14:30',
    modifiedTime: null,
    teacher: '—'
  },
  {
    id: 'APP20260901002',
    studentId: '20230004',
    studentName: '赵磊',
    college: '农学院',
    major: '农学',
    originScore: 'K',
    currentScore: 'K',
    applyReason: '参加学校运动会请假，未请假手续现已补齐',
    status: 'approved',
    statusText: '已同意',
    applyTime: '2026-09-07 10:15',
    modifiedTime: null,
    teacher: '王教授'
  },
  {
    id: 'APP20260901003',
    studentId: '20230008',
    studentName: '吴伟',
    college: '园艺学院',
    major: '园艺',
    originScore: '0',
    currentScore: '0',
    applyReason: '当天因事迟到，已补完成劳动任务',
    status: 'pending',
    statusText: '待审核',
    applyTime: '2026-09-09 16:00',
    modifiedTime: null,
    teacher: '—'
  },
  {
    id: 'APP20260901004',
    studentId: '20230001',
    studentName: '张明',
    college: '农学院',
    major: '农学',
    originScore: '5',
    currentScore: '9',
    applyReason: '考勤记录登记错误，应为 9 分',
    status: 'modified',
    statusText: '已修改',
    applyTime: '2026-09-06 09:45',
    // 模拟 0.5 天前修改完成，剩余 2.5 天
    modifiedTime: Date.now() - 0.5 * 24 * 60 * 60 * 1000,
    teacher: '李老师'
  }
]

// 可选授课教师列表（用于修改申请页选择审批人）
const teachers = ['王教授', '李老师', '陈教授', '赵老师']

// 班级选择器选项
const classOptions = {
  days: ['周一', '周二', '周三', '周四', '周五'],
  periods: ['12节', '34节', '56节', '78节', '910节'],
  locations: ['茶园', '果园', '麦田', '蔬菜大棚', '苗圃基地']
}

module.exports = {
  students,
  modifyApplications,
  classOptions,
  teachers
}
