import axios, { type AxiosInstance, type AxiosResponse, type InternalAxiosRequestConfig } from 'axios';

/**
 * 后端统一返回结构
 */
export interface CommonResult<T = unknown> {
  code: number;
  message: string;
  data: T;
  timestamp?: number;
}

const TOKEN_KEY = 'labor_admin_token';

/**
 * 获取本地 Token
 */
export function getToken(): string | null {
  return localStorage.getItem(TOKEN_KEY);
}

/**
 * 保存本地 Token
 */
export function setToken(token: string): void {
  localStorage.setItem(TOKEN_KEY, token);
}

/**
 * 清除本地 Token
 */
export function clearToken(): void {
  localStorage.removeItem(TOKEN_KEY);
}

// 创建 axios 实例
const request: AxiosInstance = axios.create({
  baseURL: '/api',
  timeout: 15000,
});

// 请求拦截器：添加 Authorization 头
request.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = getToken();
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// 响应拦截器：处理 401 跳转登录
request.interceptors.response.use(
  (response: AxiosResponse<CommonResult>) => {
    const result = response.data;
    // 业务层未认证
    if (result.code === 401) {
      clearToken();
      redirectToLogin();
      return Promise.reject(new Error(result.message || '未登录或登录已过期'));
    }
    return response;
  },
  (error) => {
    const status = error?.response?.status;
    if (status === 401) {
      clearToken();
      redirectToLogin();
    }
    return Promise.reject(error);
  }
);

function redirectToLogin(): void {
  const current = window.location.pathname;
  if (current !== '/login') {
    window.location.href = '/login';
  }
}

export default request;
