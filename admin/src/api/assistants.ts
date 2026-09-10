import request, { type CommonResult } from '@/utils/request';
import type { MasterListViewVO } from './masterList';

/** 查询未分配（负责班级）的助教列表 */
export function getUnassignedAssistants(): Promise<CommonResult<MasterListViewVO[]>> {
  return request
    .get<CommonResult<MasterListViewVO[]>>('/assistants/unassigned')
    .then((res) => res.data);
}

/** 分配助教到班级（请求体：{ classId }） */
export function assignAssistant(
  studentId: number,
  classId: number
): Promise<CommonResult<null>> {
  return request
    .put<CommonResult<null>>(`/assistants/${studentId}/assign`, { classId })
    .then((res) => res.data);
}

/** 取消助教的班级分配 */
export function unassignAssistant(studentId: number): Promise<CommonResult<null>> {
  return request
    .put<CommonResult<null>>(`/assistants/${studentId}/unassign`)
    .then((res) => res.data);
}
