import request, { type CommonResult } from '@/utils/request';

/** 班级 VO */
export interface ClassVO {
  id: number;
  className: string;
  classCode: string;
  academicYear: string;
  status: number;
  studentCount: number;
  createdAt?: string;
}

/** 班级分页结果 */
export interface ClassPage {
  records: ClassVO[];
  total: number;
  current: number;
  size: number;
}

/** 班级新增 DTO */
export interface ClassCreateDTO {
  className: string;
  classCode: string;
  academicYear: string;
}

/** 班级修改 DTO */
export interface ClassUpdateDTO extends ClassCreateDTO {
  id: number;
  status: number;
}

/** 分页查询班级列表 */
export function getClassPage(page = 1, size = 10): Promise<CommonResult<ClassPage>> {
  return request.get<CommonResult<ClassPage>>('/classes/page', { params: { page, size } }).then((res) => res.data);
}

/** 查询所有启用的班级（下拉框用） */
export function getClassList(): Promise<CommonResult<ClassVO[]>> {
  return request.get<CommonResult<ClassVO[]>>('/classes/list').then((res) => res.data);
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
