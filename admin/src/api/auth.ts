import request, { type CommonResult } from '@/utils/request';

/**
 * 登录用户信息
 */
export interface UserInfo {
  id: number;
  username: string;
  realName: string;
  roles: string[];
}

/**
 * 登录返回结果
 */
export interface LoginVO {
  token: string;
  userInfo: UserInfo;
  firstLogin: boolean;
}

/**
 * 登录请求参数
 */
export interface LoginDTO {
  username: string;
  password: string;
}

/**
 * 修改密码请求参数
 */
export interface ChangePasswordDTO {
  oldPassword: string;
  newPassword: string;
  confirmPassword: string;
}

/**
 * 登录
 */
export function login(data: LoginDTO): Promise<CommonResult<LoginVO>> {
  return request.post<CommonResult<LoginVO>>('/auth/login', data).then((res) => res.data);
}

/**
 * 修改密码
 */
export function changePassword(data: ChangePasswordDTO): Promise<CommonResult<null>> {
  return request.post<CommonResult<null>>('/auth/change-password', data).then((res) => res.data);
}
