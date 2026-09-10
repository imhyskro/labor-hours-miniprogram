import { defineStore } from 'pinia';
import { ref, computed } from 'vue';
import {
  login as loginApi,
  changePassword as changePasswordApi,
  type LoginDTO,
  type ChangePasswordDTO,
  type UserInfo,
} from '@/api/auth';
import { setToken, clearToken, getToken } from '@/utils/request';

/**
 * 用户状态 Store
 *
 * <p>管理 token、用户信息、firstLogin 状态，并同步 localStorage。</p>
 */
export const useUserStore = defineStore('user', () => {
  const TOKEN_KEY = 'labor_admin_token';
  const USER_KEY = 'labor_admin_user';
  const FIRST_LOGIN_KEY = 'labor_admin_first_login';

  /** Token */
  const token = ref<string | null>(getToken());

  /** 用户信息 */
  const userInfo = ref<UserInfo | null>(loadUserInfo());

  /** 是否首次登录（需强制修改密码） */
  const firstLogin = ref<boolean>(loadFirstLogin());

  /** 当前用户是否为超级管理员 */
  const isSuperAdmin = computed(() => {
    return userInfo.value?.roles?.includes('SUPER_ADMIN') ?? false;
  });

  function loadUserInfo(): UserInfo | null {
    const raw = localStorage.getItem(USER_KEY);
    if (!raw) return null;
    try {
      return JSON.parse(raw) as UserInfo;
    } catch {
      return null;
    }
  }

  function loadFirstLogin(): boolean {
    return localStorage.getItem(FIRST_LOGIN_KEY) === 'true';
  }

  function persistUserInfo(info: UserInfo): void {
    userInfo.value = info;
    localStorage.setItem(USER_KEY, JSON.stringify(info));
  }

  function persistFirstLogin(val: boolean): void {
    firstLogin.value = val;
    localStorage.setItem(FIRST_LOGIN_KEY, String(val));
  }

  /**
   * 初始化：从 localStorage 恢复状态（刷新页面后调用）
   */
  function init(): void {
    token.value = getToken();
    userInfo.value = loadUserInfo();
    firstLogin.value = loadFirstLogin();
  }

  /**
   * 登录
   */
  async function login(dto: LoginDTO): Promise<void> {
    const result = await loginApi(dto);
    if (result.code !== 200) {
      throw new Error(result.message || '登录失败');
    }
    const data = result.data;
    token.value = data.token;
    setToken(data.token);
    persistUserInfo(data.userInfo);
    persistFirstLogin(data.firstLogin);
  }

  /**
   * 修改密码
   */
  async function changePassword(dto: ChangePasswordDTO): Promise<void> {
    const result = await changePasswordApi(dto);
    if (result.code !== 200) {
      throw new Error(result.message || '密码修改失败');
    }
    persistFirstLogin(false);
  }

  /**
   * 退出登录
   */
  function logout(): void {
    token.value = null;
    userInfo.value = null;
    firstLogin.value = false;
    clearToken();
    localStorage.removeItem(USER_KEY);
    localStorage.removeItem(FIRST_LOGIN_KEY);
    // 跳转到登录页
    window.location.href = '/login';
  }

  return {
    token,
    userInfo,
    firstLogin,
    isSuperAdmin,
    init,
    login,
    changePassword,
    logout,
  };
});
