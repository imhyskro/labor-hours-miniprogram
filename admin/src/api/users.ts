import request, { type CommonResult } from '@/utils/request';

/** 用户 VO */
export interface UserVO {
  id: number;
  username: string;
  realName: string;
  roles: string[];
  status: number;
  firstLogin: boolean;
  lastPasswordChangeTime?: string;
  createdAt?: string;
}

/** 用户分页结果 */
export interface UserPage {
  records: UserVO[];
  total: number;
  current: number;
  size: number;
}

/** 用户查询参数 */
export interface UserQueryParams {
  keyword?: string;
  status?: number | null;
  page?: number;
  size?: number;
}

/** 分页查询用户列表 */
export function getUserPage(params: UserQueryParams): Promise<CommonResult<UserPage>> {
  return request.get<CommonResult<UserPage>>('/admin/users/page', { params }).then((res) => res.data);
}

/** 启用/停用用户 */
export function updateUserStatus(userId: number, status: number): Promise<CommonResult<null>> {
  return request.put<CommonResult<null>>(`/admin/users/${userId}/status`, null, {
    params: { status },
  }).then((res) => res.data);
}

/** 重置用户密码为默认值 */
export function resetUserPassword(userId: number): Promise<CommonResult<null>> {
  return request.put<CommonResult<null>>(`/admin/users/${userId}/reset-password`).then((res) => res.data);
}
