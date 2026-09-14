import request, { type CommonResult } from '@/utils/request';

/** 助教 VO */
export interface AssistantVO {
  id: number;
  studentId: string;
  name: string;
  gender: number;
  originalMajor: string | null;
  companyName: string | null;
  classCode: string | null;
  className: string | null;
  studentNoInClass: number | null;
  fullNo: string | null;
  hasAccount: number;
  assignedClassCount: number;
  assignedClassNames: string | null;
}

/** 助教分页结果 */
export interface AssistantPage {
  records: AssistantVO[];
  total: number;
  current: number;
  size: number;
}

/** 助教分页查询 */
export function getAssistantPage(
  page = 1,
  size = 10,
  keyword?: string
): Promise<CommonResult<AssistantPage>> {
  return request
    .get<CommonResult<AssistantPage>>('/assistants/page', { params: { page, size, keyword } })
    .then((res) => res.data);
}

/** 查询助教当前负责的班级ID列表 */
export function getAssistantClassIds(studentId: number): Promise<CommonResult<number[]>> {
  return request
    .get<CommonResult<number[]>>(`/assistants/${studentId}/classes`)
    .then((res) => res.data);
}

/** 设置助教负责的班级（全量覆盖，classIds 为空数组表示清空） */
export function assignAssistantClasses(
  studentId: number,
  classIds: number[]
): Promise<CommonResult<null>> {
  return request
    .put<CommonResult<null>>(`/assistants/${studentId}/classes`, { classIds })
    .then((res) => res.data);
}

/** 取消助教身份，恢复为普通学生 */
export function revokeAssistant(studentId: number): Promise<CommonResult<null>> {
  return request
    .put<CommonResult<null>>(`/assistants/${studentId}/revoke`)
    .then((res) => res.data);
}
