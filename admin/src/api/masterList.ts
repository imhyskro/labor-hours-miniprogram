import request, { type CommonResult } from '@/utils/request';

/** 总表视图行 */
export interface MasterListViewVO {
  id: number;
  studentId: string;
  name: string;
  gender: number;
  classId: number;
  className: string;
  isAssistant: number;
  identity: string;
  assignedClassId: number | null;
  assignedClassName: string | null;
  hasAccount: number;
}

/** 总表分页结果 */
export interface MasterListPage {
  records: MasterListViewVO[];
  total: number;
  current: number;
  size: number;
}

/** 总表查询参数 */
export interface MasterListQuery {
  page?: number;
  size?: number;
  keyword?: string;
  classId?: number | null;
  identity?: string | null;
}

/** 总表分页查询 */
export function getMasterList(params: MasterListQuery): Promise<CommonResult<MasterListPage>> {
  return request
    .get<CommonResult<MasterListPage>>('/master-list', { params })
    .then((res) => res.data);
}
