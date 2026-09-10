import request, { type CommonResult } from '@/utils/request';

/** 学生 VO */
export interface StudentVO {
  id: number;
  studentId: string;
  name: string;
  classId: number;
  className: string;
  gender: number;
  status: number;
  createdAt?: string;
}

/** 学生分页结果 */
export interface StudentPage {
  records: StudentVO[];
  total: number;
  current: number;
  size: number;
}

/** 学生查询参数 */
export interface StudentQueryParams {
  keyword?: string;
  classId?: number | null;
  page?: number;
  size?: number;
}

/** 学生新增 DTO */
export interface StudentCreateDTO {
  studentId: string;
  name: string;
  classId: number;
  gender: number;
}

/** 学生修改 DTO */
export interface StudentUpdateDTO extends StudentCreateDTO {
  id: number;
  status: number;
}

/** 分页查询学生列表 */
export function getStudentPage(params: StudentQueryParams): Promise<CommonResult<StudentPage>> {
  return request.get<CommonResult<StudentPage>>('/students/page', { params }).then((res) => res.data);
}

/** 新增学生 */
export function createStudent(data: StudentCreateDTO): Promise<CommonResult<null>> {
  return request.post<CommonResult<null>>('/students', data).then((res) => res.data);
}

/** 修改学生 */
export function updateStudent(id: number, data: StudentUpdateDTO): Promise<CommonResult<null>> {
  return request.put<CommonResult<null>>(`/students/${id}`, data).then((res) => res.data);
}

/** 删除学生 */
export function deleteStudent(id: number): Promise<CommonResult<null>> {
  return request.delete<CommonResult<null>>(`/students/${id}`).then((res) => res.data);
}
