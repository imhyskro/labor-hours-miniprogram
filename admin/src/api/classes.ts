import request, { type CommonResult } from '@/utils/request';

/** 班级 VO */
export interface ClassVO {
  id: number;
  companyId?: number;
  companyName?: string;
  className: string;
  /** 班级编码 W-S-E */
  classCode: string | null;
  /** 周次 W */
  week?: number;
  /** 开始节次 S */
  startSession?: number;
  /** 结束节次 E */
  endSession?: number;
  academicYear?: string;
  status: number;
  studentCount: number;
  createdAt?: string;
}

/** 班级内学生 VO */
export interface ClassStudentVO {
  id: number;
  studentId: string;
  name: string;
  studentNoInClass: number | null;
  fullNo: string | null;
  originalMajor: string | null;
  gender: number;
  isAssistant: number;
}

/** 班级分页结果 */
export interface ClassPage {
  records: ClassVO[];
  total: number;
  current: number;
  size: number;
}

/** 班级新增 DTO（公司 + 周次 + 开始节次 + 结束节次，编码名称自动生成） */
export interface ClassCreateDTO {
  companyId: number;
  week: number;
  startSession: number;
  endSession: number;
  academicYear?: string;
}

/** 班级修改 DTO */
export interface ClassUpdateDTO {
  week: number;
  startSession: number;
  endSession: number;
  status?: number;
}

/** 分页查询班级列表 */
export function getClassPage(page = 1, size = 10): Promise<CommonResult<ClassPage>> {
  return request.get<CommonResult<ClassPage>>('/classes/page', { params: { page, size } }).then((res) => res.data);
}

/** 查询所有班级（下拉/筛选用，含公司名与周次节次） */
export function getClassList(): Promise<CommonResult<ClassVO[]>> {
  return request.get<CommonResult<ClassVO[]>>('/classes/list').then((res) => res.data);
}

/** 查询某公司下的所有班级（按周次-节次排序） */
export function getClassesByCompany(companyId: number): Promise<CommonResult<ClassVO[]>> {
  return request
    .get<CommonResult<ClassVO[]>>(`/classes/company/${companyId}`)
    .then((res) => res.data);
}

/** 查询某班级的学生列表（按班内编号排序） */
export function getClassStudents(classId: number): Promise<CommonResult<ClassStudentVO[]>> {
  return request
    .get<CommonResult<ClassStudentVO[]>>(`/classes/${classId}/students`)
    .then((res) => res.data);
}

/** 新增班级 */
export function createClass(data: ClassCreateDTO): Promise<CommonResult<null>> {
  return request.post<CommonResult<null>>('/classes', data).then((res) => res.data);
}

/** 修改班级 */
export function updateClass(id: number, data: ClassUpdateDTO): Promise<CommonResult<null>> {
  return request.put<CommonResult<null>>(`/classes/${id}`, data).then((res) => res.data);
}

/** 删除班级 */
export function deleteClass(id: number): Promise<CommonResult<null>> {
  return request.delete<CommonResult<null>>(`/classes/${id}`).then((res) => res.data);
}
