<script setup lang="ts">
import { nextTick, onMounted, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import {
  getAssistantPage,
  getAssistantClassIds,
  assignAssistantClasses,
  revokeAssistant,
  type AssistantVO,
} from '@/api/assistants';
import { getClassList, type ClassVO } from '@/api/classes';

const loading = ref(false);
const list = ref<AssistantVO[]>([]);
const total = ref(0);
const queryParams = ref({ page: 1, size: 10, keyword: '' });

// 分配班级弹窗
const assignVisible = ref(false);
const assigning = ref(false);
const allClasses = ref<ClassVO[]>([]);
const currentAssistant = ref<AssistantVO | null>(null);
const classTableRef = ref<InstanceType<typeof import('element-plus')['ElTable']> | null>(null);

async function loadList(): Promise<void> {
  loading.value = true;
  try {
    const res = await getAssistantPage(
      queryParams.value.page,
      queryParams.value.size,
      queryParams.value.keyword || undefined
    );
    list.value = res.data?.records ?? [];
    total.value = res.data?.total ?? 0;
  } finally {
    loading.value = false;
  }
}

function handleSearch(): void {
  queryParams.value.page = 1;
  loadList();
}

async function openAssign(row: AssistantVO): Promise<void> {
  currentAssistant.value = row;
  assignVisible.value = true;
  assigning.value = true;
  try {
    const [classRes, idsRes] = await Promise.all([getClassList(), getAssistantClassIds(row.id)]);
    allClasses.value = classRes.data ?? [];
    const selectedIds = new Set(idsRes.data ?? []);
    await nextTick();
    allClasses.value.forEach((c) => {
      if (selectedIds.has(c.id)) {
        classTableRef.value?.toggleRowSelection(c, true);
      }
    });
  } finally {
    assigning.value = false;
  }
}

async function submitAssign(): Promise<void> {
  if (!currentAssistant.value) return;
  const selected = classTableRef.value?.getSelectionRows() as ClassVO[] | undefined;
  const ids = (selected ?? []).map((c) => c.id);
  try {
    await assignAssistantClasses(currentAssistant.value.id, ids);
    ElMessage.success('负责班级已更新');
    assignVisible.value = false;
    await loadList();
  } catch {
    /* 错误已由拦截器提示 */
  }
}

async function handleRevoke(row: AssistantVO): Promise<void> {
  try {
    await ElMessageBox.confirm(
      `确定取消「${row.name}」的助教身份吗？将同时停用其登录账号，其学生记录保留。`,
      '取消助教',
      { type: 'warning', confirmButtonText: '取消助教', cancelButtonText: '返回' }
    );
  } catch {
    return;
  }
  try {
    await revokeAssistant(row.id);
    ElMessage.success('已恢复为普通学生');
    await loadList();
  } catch {
    /* 错误已由拦截器提示 */
  }
}

function genderText(g: number): string {
  return g === 1 ? '男' : g === 2 ? '女' : '未知';
}

onMounted(loadList);
</script>

<template>
  <div v-loading="loading">
    <div class="page-header">
      <h2 class="page-title">助教管理</h2>
      <div class="search-bar">
        <el-input
          v-model="queryParams.keyword"
          placeholder="搜索学号 / 姓名"
          clearable
          style="width: 240px"
          @keyup.enter="handleSearch"
          @clear="handleSearch"
        />
        <el-button type="primary" @click="handleSearch">查询</el-button>
      </div>
    </div>

    <el-table :data="list" border stripe>
      <el-table-column label="学号" prop="studentId" width="130" />
      <el-table-column label="姓名" prop="name" width="100" />
      <el-table-column label="原始专业" prop="originalMajor" min-width="150" show-overflow-tooltip>
        <template #default="{ row }">{{ row.originalMajor || '—' }}</template>
      </el-table-column>
      <el-table-column label="所属公司" prop="companyName" width="100">
        <template #default="{ row }">{{ row.companyName || '—' }}</template>
      </el-table-column>
      <el-table-column label="班级 (W-S-E)" prop="classCode" width="110" align="center">
        <template #default="{ row }">{{ row.classCode || '—' }}</template>
      </el-table-column>
      <el-table-column label="班内编号" prop="studentNoInClass" width="90" align="center">
        <template #default="{ row }">{{ row.studentNoInClass ?? '—' }}</template>
      </el-table-column>
      <el-table-column label="性别" width="70" align="center">
        <template #default="{ row }">{{ genderText(row.gender) }}</template>
      </el-table-column>
      <el-table-column label="负责班级数" prop="assignedClassCount" width="90" align="center" />
      <el-table-column label="负责班级" prop="assignedClassNames" min-width="200" show-overflow-tooltip>
        <template #default="{ row }">
          <span v-if="row.assignedClassNames">{{ row.assignedClassNames }}</span>
          <el-tag v-else type="info" size="small">未分配</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="180" align="center" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click="openAssign(row)">分配班级</el-button>
          <el-button link type="danger" size="small" @click="handleRevoke(row)">取消助教</el-button>
        </template>
      </el-table-column>
      <template #empty>
        <el-empty description="暂无助教，请通过「导入助教」批量添加" />
      </template>
    </el-table>

    <el-pagination
      class="pager"
      layout="total, prev, pager, next"
      :total="total"
      :page-size="queryParams.size"
      :current-page="queryParams.page"
      @current-change="(p: number) => { queryParams.page = p; loadList(); }"
    />

    <!-- 分配班级弹窗 -->
    <el-dialog
      v-model="assignVisible"
      :title="`分配负责班级 — ${currentAssistant?.name ?? ''}`"
      width="720px"
    >
      <div v-loading="assigning">
        <el-alert
          type="info"
          :closable="false"
          title="勾选该助教负责的班级（可多选），保存后全量覆盖原分配。"
          style="margin-bottom: 12px"
        />
        <el-table
          ref="classTableRef"
          :data="allClasses"
          border
          stripe
          max-height="420"
          row-key="id"
        >
          <el-table-column type="selection" width="50" align="center" />
          <el-table-column label="公司" prop="companyName" width="120" />
          <el-table-column label="周次" prop="week" width="70" align="center" />
          <el-table-column label="节次" width="120" align="center">
            <template #default="{ row }">{{ row.startSession }}~{{ row.endSession }}</template>
          </el-table-column>
          <el-table-column label="班级编码" prop="classCode" width="110" align="center" />
          <el-table-column label="班级名称" prop="className" min-width="200" />
        </el-table>
      </div>
      <template #footer>
        <el-button @click="assignVisible = false">取消</el-button>
        <el-button type="primary" @click="submitAssign">保存分配</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
  flex-wrap: wrap;
  gap: 12px;
}
.page-title {
  margin: 0;
  font-size: 18px;
}
.search-bar {
  display: flex;
  gap: 8px;
}
.pager {
  margin-top: 16px;
  justify-content: flex-end;
}
</style>
