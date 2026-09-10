<script setup lang="ts">
import { computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useUserStore } from '@/stores/userStore';

const route = useRoute();
const router = useRouter();
const userStore = useUserStore();

/** 当前激活的菜单项（根据路由 path 高亮） */
const activeMenu = computed(() => '/' + (route.path.split('/')[1] || 'dashboard'));

/** 当前用户真实姓名 */
const realName = computed(() => userStore.userInfo?.realName || '用户');

/** 下拉菜单命令处理 */
function handleCommand(command: string): void {
  if (command === 'change-password') {
    router.push('/change-password');
  } else if (command === 'logout') {
    userStore.logout();
  }
}

/** 菜单项跳转 */
function handleMenuSelect(index: string): void {
  router.push(index);
}
</script>

<template>
  <el-container class="layout-container">
    <!-- 顶部导航栏 -->
    <el-header class="layout-header">
      <div class="header-left">
        <span class="system-title">劳动学时管理系统</span>
      </div>
      <div class="header-right">
        <el-dropdown @command="handleCommand">
          <span class="user-info">
            <el-icon class="user-icon"><UserFilled /></el-icon>
            <span class="user-name">{{ realName }}</span>
            <el-icon class="arrow-icon"><ArrowDown /></el-icon>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="change-password">
                <el-icon><Lock /></el-icon>
                修改密码
              </el-dropdown-item>
              <el-dropdown-item command="logout" divided>
                <el-icon><SwitchButton /></el-icon>
                退出登录
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </el-header>

    <el-container class="main-container">
      <!-- 左侧侧边栏 -->
      <el-aside width="220px" class="layout-aside">
        <el-menu
          :default-active="activeMenu"
          class="side-menu"
          background-color="#304156"
          text-color="#bfcbd9"
          active-text-color="#409EFF"
          @select="handleMenuSelect"
        >
          <el-menu-item index="/dashboard">
            <el-icon><Monitor /></el-icon>
            <span>首页</span>
          </el-menu-item>

          <el-sub-menu index="base-data">
            <template #title>
              <el-icon><Folder /></el-icon>
              <span>基础数据管理</span>
            </template>
            <el-menu-item index="/classes">
              <el-icon><OfficeBuilding /></el-icon>
              <span>班级管理</span>
            </el-menu-item>
            <el-menu-item index="/students">
              <el-icon><User /></el-icon>
              <span>学生管理</span>
            </el-menu-item>
            <el-menu-item index="/import/students">
              <el-icon><Upload /></el-icon>
              <span>导入学生</span>
            </el-menu-item>
            <el-menu-item index="/import/assistants">
              <el-icon><UserFilled /></el-icon>
              <span>导入助教</span>
            </el-menu-item>
            <el-menu-item index="/master-list">
              <el-icon><Document /></el-icon>
              <span>学生总表</span>
            </el-menu-item>
            <el-menu-item index="/assistants/assignment">
              <el-icon><Connection /></el-icon>
              <span>助教分配</span>
            </el-menu-item>
          </el-sub-menu>

          <el-sub-menu v-if="userStore.isSuperAdmin" index="system">
            <template #title>
              <el-icon><Tools /></el-icon>
              <span>系统管理</span>
            </template>
            <el-menu-item index="/users">
              <el-icon><Setting /></el-icon>
              <span>用户管理</span>
            </el-menu-item>
          </el-sub-menu>
        </el-menu>
      </el-aside>

      <!-- 右侧主内容区 -->
      <el-main class="layout-main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<style scoped lang="scss">
.layout-container {
  height: 100vh;
}

.layout-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 60px;
  background-color: #ffffff;
  border-bottom: 1px solid #e6e6e6;
  padding: 0 20px;
  box-sizing: border-box;
}

.header-left {
  display: flex;
  align-items: center;
}

.system-title {
  font-weight: 600;
  font-size: 18px;
  color: #333;
  margin-left: 16px;
}

.header-right {
  display: flex;
  align-items: center;
}

.user-info {
  display: flex;
  align-items: center;
  cursor: pointer;
  color: #333;
  outline: none;
  .user-icon {
    margin-right: 6px;
    font-size: 16px;
  }
  .user-name {
    font-size: 14px;
  }
  .arrow-icon {
    margin-left: 4px;
    font-size: 12px;
  }
}

.main-container {
  height: calc(100vh - 60px);
}

.layout-aside {
  background-color: #304156;
  overflow-x: hidden;
}

.side-menu {
  border-right: none;
  height: 100%;
}

:deep(.el-menu) {
  background-color: #304156;
}

:deep(.el-sub-menu__title:hover),
:deep(.el-menu-item:hover) {
  background-color: #263445 !important;
}

:deep(.el-menu-item.is-active) {
  background-color: rgba(64, 158, 255, 0.2) !important;
}

.layout-main {
  background-color: #f0f2f5;
  padding: 20px;
  overflow-y: auto;
}
</style>
