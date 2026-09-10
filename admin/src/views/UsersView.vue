<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import {
  getUserPage,
  updateUserStatus,
  resetUserPassword,
  type UserVO,
} from '@/api/users';
import { useUserStore } from '@/stores/userStore';

const userStore = useUserStore();

const loading = ref(false);
const tableData = ref<UserVO[]>([]);
const total = ref(0);
const currentPage = ref(1);
const pageSize = ref(10);

// 筛选条件
const searchKeyword = ref('');
const filterStatus = ref<number | undefined>(undefined);

/** 当前登录用户 ID（用于禁用对自己账号的操作） */
const currentUserId = computed(() => userStore.userInfo?.id);

async function loadData() {
  loading.value = true;
  try {
    const result = await getUserPage({
      keyword: searchKeyword.value || undefined,
      status: filterStatus.value ?? undefined,
      page: currentPage.value,
      size: pageSize.value,
    });
    if (result.code === 200) {
      tableData.value = result.data.records;
      total.value = result.data.total;
    }
  } catch {
    ElMessage.error('查询失败');
  } finally {
    loading.value = false;
  }
}

function handleSearch() {
  currentPage.value = 1;
  loadData();
}

function handleResetSearch() {
  searchKeyword.value = '';
  filterStatus.value = undefined;
  currentPage.value = 1;
  loadData();
}

/** 是否当前登录用户自己 */
function isSelf(row: UserVO): boolean {
  return currentUserId.value === row.id;
}

/** 角色编码转中文标签 */
function roleLabel(code: string): string {
  switch (code) {
    case 'SUPER_ADMIN': return '超级管理员';
    case 'TEACHER': return '教师';
    case 'ASSISTANT': return '助教';
    default: return code;
  }
}

/** 角色标签颜色 */
function roleTagType(code: string): 'danger' | 'warning' | 'success' | 'info' {
  switch (code) {
    case 'SUPER_ADMIN': return 'danger';
    case 'TEACHER': return 'warning';
    case 'ASSISTANT': return 'success';
    default: return 'info';
  }
}

/** 状态标签颜色 */
function statusTagType(status: number): 'success' | 'info' {
  return status === 1 ? 'success' : 'info';
}

/** 状态文本 */
function statusText(status: number): string {
  return status === 1 ? '启用' : '停用';
}

/** 首次登录标签颜色 */
function firstLoginTagType(firstLogin: boolean): 'warning' | 'info' {
  return firstLogin ? 'warning' : 'info';
}

/** 切换用户状态（启用/停用） */
async function handleToggleStatus(row: UserVO) {
  if (isSelf(row)) {
    ElMessage.warning('不能停用自己的账号');
    return;
  }
  const targetStatus = row.status === 1 ? 0 : 1;
  const actionText = targetStatus === 1 ? '启用' : '停用';
  try {
    await ElMessageBox.confirm(`确定要${actionText}用户「${row.realName}」吗？`, '提示', {
      type: 'warning',
      confirmButtonText: '确定',
      cancelButtonText: '取消',
    });
    const result = await updateUserStatus(row.id, targetStatus);
    if (result.code === 200) {
      ElMessage.success(`${actionText}成功`);
      await loadData();
    } else {
      ElMessage.error(result.message);
    }
  } catch {
    // 用户取消
  }
}

/** 重置密码 */
async function handleResetPassword(row: UserVO) {
  if (isSelf(row)) {
    ElMessage.warning('不能重置自己的密码');
    return;
  }
  try {
    await ElMessageBox.confirm(
      `确定要重置用户「${row.realName}」的密码吗？重置后密码为 cdjcc123456，该用户下次登录需强制修改密码。`,
      '重置密码确认',
      {
        type: 'warning',
        confirmButtonText: '确定重置',
        cancelButtonText: '取消',
      }
    );
    const result = await resetUserPassword(row.id);
    if (result.code === 200) {
      ElMessage.success('密码已重置为 cdjcc123456，该用户下次登录需强制修改密码');
      await loadData();
    } else {
      ElMessage.error(result.message);
    }
  } catch {
    // 用户取消
  }
}

function handlePageChange(page: number) {
  currentPage.value = page;
  loadData();
}

function handleSizeChange(size: number) {
  pageSize.value = size;
  currentPage.value = 1;
  loadData();
}

onMounted(() => {
  loadData();
});
</script>

<template>
  <div class="users-container">
    <el-card shadow="never">
      <div class="toolbar">
        <span class="page-title">用户管理</span>
      </div>

      <div class="search-bar">
        <el-input
          v-model="searchKeyword"
          placeholder="按用户名/姓名搜索"
          clearable
          style="width: 240px"
          @keyup.enter="handleSearch"
          @clear="handleSearch"
        />
        <el-select
          v-model="filterStatus"
          placeholder="按状态筛选"
          clearable
          style="width: 160px"
          @change="handleSearch"
        >
          <el-option label="启用" :value="1" />
          <el-option label="停用" :value="0" />
        </el-select>
        <el-button type="primary" @click="handleSearch">
          <el-icon><Search /></el-icon>
          搜索
        </el-button>
        <el-button @click="handleResetSearch">重置</el-button>
      </div>

      <el-table :data="tableData" v-loading="loading" border style="width: 100%">
        <el-table-column type="index" label="序号" width="60" align="center" />
        <el-table-column prop="username" label="用户名" width="140" />
        <el-table-column prop="realName" label="真实姓名" width="120" />
        <el-table-column label="角色" min-width="180">
          <template #default="{ row }">
            <el-tag
              v-for="role in row.roles"
              :key="role"
              :type="roleTagType(role)"
              size="small"
              style="margin-right: 6px"
            >
              {{ roleLabel(role) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" size="small">
              {{ statusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="首次登录" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="firstLoginTagType(row.firstLogin)" size="small">
              {{ row.firstLogin ? '是' : '否' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="lastPasswordChangeTime" label="最后改密时间" width="170" />
        <el-table-column prop="createdAt" label="创建时间" width="170" />
        <el-table-column label="操作" width="200" align="center" fixed="right">
          <template #default="{ row }">
            <el-button
              type="warning"
              link
              :disabled="isSelf(row)"
              @click="handleResetPassword(row)"
            >
              重置密码
            </el-button>
            <el-button
              :type="row.status === 1 ? 'danger' : 'success'"
              link
              :disabled="isSelf(row)"
              @click="handleToggleStatus(row)"
            >
              {{ row.status === 1 ? '停用' : '启用' }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          @current-change="handlePageChange"
          @size-change="handleSizeChange"
        />
      </div>
    </el-card>
  </div>
</template>

<style scoped lang="scss">
.users-container {
  .toolbar {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 16px;
    .page-title {
      font-size: 18px;
      font-weight: 600;
      color: #303133;
    }
  }
  .search-bar {
    display: flex;
    align-items: center;
    gap: 12px;
    margin-bottom: 16px;
  }
  .pagination-wrapper {
    display: flex;
    justify-content: flex-end;
    margin-top: 16px;
  }
}
</style>
