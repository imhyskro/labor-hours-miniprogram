import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router';
import { useUserStore } from '@/stores/userStore';
import { getToken } from '@/utils/request';
import { ElMessage } from 'element-plus';

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/LoginView.vue'),
    meta: { public: true },
  },
  {
    path: '/change-password',
    name: 'ChangePassword',
    component: () => import('@/views/ChangePasswordView.vue'),
    meta: { requiresAuth: true, allowFirstLogin: true },
  },
  {
    path: '/',
    component: () => import('@/layout/index.vue'),
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/DashboardView.vue'),
        meta: { title: '首页', icon: 'Monitor' },
      },
      {
        path: 'classes',
        name: 'Classes',
        component: () => import('@/views/ClassesView.vue'),
        meta: { title: '班级管理', icon: 'OfficeBuilding' },
      },
      {
        path: 'students',
        name: 'Students',
        component: () => import('@/views/StudentsView.vue'),
        meta: { title: '学生管理', icon: 'User' },
      },
      {
        path: 'import/students',
        name: 'StudentImport',
        component: () => import('@/views/StudentImportView.vue'),
        meta: { title: '导入学生', icon: 'Upload' },
      },
      {
        path: 'import/assistants',
        name: 'AssistantImport',
        component: () => import('@/views/AssistantImportView.vue'),
        meta: { title: '导入助教', icon: 'UserFilled' },
      },
      {
        path: 'master-list',
        name: 'MasterList',
        component: () => import('@/views/MasterListView.vue'),
        meta: { title: '学生总表', icon: 'Document' },
      },
      {
        path: 'assistants/assignment',
        name: 'AssistantAssignment',
        component: () => import('@/views/AssignmentView.vue'),
        meta: { title: '助教分配', icon: 'Connection' },
      },
      {
        path: 'users',
        name: 'Users',
        component: () => import('@/views/UsersView.vue'),
        meta: { title: '用户管理', icon: 'Setting', roles: ['SUPER_ADMIN'] },
      },
    ],
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/dashboard',
  },
];

const router = createRouter({
  history: createWebHistory(),
  routes,
});

/**
 * 路由守卫
 *
 * - /login：有 token 则重定向 /dashboard，否则放行
 * - /change-password：firstLogin=true 时放行，否则重定向 /dashboard
 * - 其他需认证页面：无 token 跳 /login；firstLogin 时强制跳 /change-password
 * - 需要 roles 的页面：无权限跳 /dashboard 并提示
 */
router.beforeEach((to, _from, next) => {
  const hasToken = !!getToken();
  const userStore = useUserStore();

  // 访问登录页：有 token 重定向 /dashboard，否则放行
  if (to.path === '/login') {
    if (hasToken) {
      next('/dashboard');
    } else {
      next();
    }
    return;
  }

  // 首次登录强制改密页
  if (to.path === '/change-password') {
    if (!hasToken) {
      next('/login');
    } else if (userStore.firstLogin) {
      next();
    } else {
      next('/dashboard');
    }
    return;
  }

  // 其他所有页面（Layout 下的子路由 + 兜底重定向）均需认证
  if (!hasToken) {
    next('/login');
    return;
  }
  if (userStore.firstLogin) {
    next('/change-password');
    return;
  }

  // 角色校验
  const requiredRoles = to.meta.roles as string[] | undefined;
  if (requiredRoles && requiredRoles.length > 0) {
    const userRoles = userStore.userInfo?.roles ?? [];
    const hasRole = requiredRoles.some((r) => userRoles.includes(r));
    if (!hasRole) {
      ElMessage.error('无权限访问');
      next('/dashboard');
      return;
    }
  }

  next();
});

export default router;
