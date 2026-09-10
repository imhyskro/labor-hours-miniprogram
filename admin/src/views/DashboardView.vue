<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue';
import { useUserStore } from '@/stores/userStore';

const userStore = useUserStore();

/** 当前系统时间（每秒刷新） */
const currentTime = ref<string>(formatTime(new Date()));
let timer: ReturnType<typeof setInterval> | null = null;

/** 当前用户姓名 */
const realName = computed(() => userStore.userInfo?.realName || '用户');

/** 当前用户角色标签文本 */
const roleText = computed(() => {
  const roles = userStore.userInfo?.roles ?? [];
  if (roles.includes('SUPER_ADMIN')) return '超级管理员';
  if (roles.includes('TEACHER')) return '教师';
  if (roles.includes('ASSISTANT')) return '助教';
  return '未知角色';
});

/** 角色标签类型（颜色） */
const roleTagType = computed<'danger' | 'warning' | 'success' | 'info'>(() => {
  const roles = userStore.userInfo?.roles ?? [];
  if (roles.includes('SUPER_ADMIN')) return 'danger';
  if (roles.includes('TEACHER')) return 'warning';
  if (roles.includes('ASSISTANT')) return 'success';
  return 'info';
});

function formatTime(date: Date): string {
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ` +
    `${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`;
}

onMounted(() => {
  timer = setInterval(() => {
    currentTime.value = formatTime(new Date());
  }, 1000);
});

onUnmounted(() => {
  if (timer) {
    clearInterval(timer);
    timer = null;
  }
});
</script>

<template>
  <div class="dashboard-container">
    <el-card class="welcome-card" shadow="never">
      <div class="welcome-content">
        <div class="welcome-title">
          <el-icon class="welcome-icon"><Bell /></el-icon>
          欢迎回来，{{ realName }}！
        </div>
        <div class="welcome-meta">
          <span class="meta-label">当前角色：</span>
          <el-tag :type="roleTagType" size="large" effect="dark">
            {{ roleText }}
          </el-tag>
        </div>
      </div>
    </el-card>

    <el-card class="info-card" shadow="never">
      <template #header>
        <div class="card-header">
          <el-icon><Clock /></el-icon>
          <span>系统时间</span>
        </div>
      </template>
      <div class="time-display">{{ currentTime }}</div>
      <div class="phase-hint">
        <el-tag type="info">阶段3：Layout 布局已完成</el-tag>
        <el-tag type="info" style="margin-left: 8px;">阶段4：班级/学生 CRUD（待开发）</el-tag>
      </div>
    </el-card>
  </div>
</template>

<style scoped lang="scss">
.dashboard-container {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.welcome-card,
.info-card {
  border-radius: 8px;
}

.welcome-content {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.welcome-title {
  display: flex;
  align-items: center;
  font-size: 22px;
  font-weight: 600;
  color: #303133;
  .welcome-icon {
    margin-right: 8px;
    font-size: 22px;
    color: #409eff;
  }
}

.welcome-meta {
  display: flex;
  align-items: center;
  .meta-label {
    color: #606266;
    font-size: 14px;
    margin-right: 8px;
  }
}

.card-header {
  display: flex;
  align-items: center;
  font-weight: 600;
  color: #303133;
  .el-icon {
    margin-right: 6px;
  }
}

.time-display {
  font-size: 28px;
  font-weight: 600;
  color: #409eff;
  text-align: center;
  padding: 20px 0;
  font-family: 'Courier New', Courier, monospace;
}

.phase-hint {
  display: flex;
  justify-content: center;
  padding: 8px 0;
}
</style>
