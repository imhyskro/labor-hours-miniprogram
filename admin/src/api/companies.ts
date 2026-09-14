import request, { type CommonResult } from '@/utils/request';

/** 公司 VO */
export interface CompanyVO {
  id: number;
  name: string;
  sortOrder: number;
  status: number;
  /** 公司下班级数 */
  classCount: number;
  createdAt?: string;
}

/** 查询所有公司（含班级数，按排序号） */
export function getCompanyList(): Promise<CommonResult<CompanyVO[]>> {
  return request.get<CommonResult<CompanyVO[]>>('/companies').then((res) => res.data);
}

/** 新增公司（只需名字） */
export function createCompany(name: string): Promise<CommonResult<null>> {
  return request.post<CommonResult<null>>('/companies', { name }).then((res) => res.data);
}

/** 公司改名 */
export function renameCompany(id: number, name: string): Promise<CommonResult<null>> {
  return request.put<CommonResult<null>>(`/companies/${id}`, { name }).then((res) => res.data);
}

/** 删除公司（公司下存在班级时后端会拒绝） */
export function deleteCompany(id: number): Promise<CommonResult<null>> {
  return request.delete<CommonResult<null>>(`/companies/${id}`).then((res) => res.data);
}
