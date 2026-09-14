import request, { type CommonResult } from '@/utils/request';

/** 总表视图行（公司 → 班级 W-S-E → 班内编号） */
export interface MasterListViewVO {
  id: number;
  studentId: string;
  name: string;
  gender: number;
  classId: number | null;
  companyId: number | null;
  companyName: string | null;
  week: number | null;
  startSession: number | null;
  endSession: number | null;
  classCode: string | null;
  className: string | null;
  studentNoInClass: number | null;
  fullNo: string | null;
  originalMajor: string | null;
  isAssistant: number;
  identity: string;
  /** 助教负责班级名称聚合（顿号分隔） */
  assignedClassNames: string | null;
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
  companyId?: number | null;
  classId?: number | null;
  identity?: string | null;
}

/** 总表分页查询 */
export function getMasterList(params: MasterListQuery): Promise<CommonResult<MasterListPage>> {
  return request
    .get<CommonResult<MasterListPage>>('/master-list', { params })
    .then((res) => res.data);
}
