<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import {
  getCompanyList,
  createCompany,
  renameCompany,
  deleteCompany,
  type CompanyVO,
} from '@/api/companies';

const router = useRouter();

const companies = ref<CompanyVO[]>([]);
const loading = ref(false);

const dialogVisible = ref(false);
const dialogTitle = ref('新增公司');
const formName = ref('');
const editingId = ref<number | null>(null);

async function loadCompanies(): Promise<void> {
  loading.value = true;
  try {
    const res = await getCompanyList();
    companies.value = res.data ?? [];
  } finally {
    loading.value = false;
  }
}

function openCreate(): void {
  editingId.value = null;
  dialogTitle.value = '新增公司';
  formName.value = '';
  dialogVisible.value = true;
}

function openRename(row: CompanyVO): void {
  editingId.value = row.id;
  dialogTitle.value = '公司改名';
  formName.value = row.name;
  dialogVisible.value = true;
}

async function submitForm(): Promise<void> {
  const name = formName.value.trim();
  if (!name) {
    ElMessage.warning('请输入公司名称');
    return;
  }
  try {
    if (editingId.value == null) {
      await createCompany(name);
      ElMessage.success('公司已添加');
    } else {
      await renameCompany(editingId.value, name);
      ElMessage.success('已改名');
    }
    dialogVisible.value = false;
    await loadCompanies();
  } catch {
    /* 错误已由拦截器提示 */
  }
}

async function handleDelete(row: CompanyVO): Promise<void> {
  try {
    await ElMessageBox.confirm(
      `确定删除公司「${row.name}」吗？公司下存在班级时无法删除。`,
      '删除确认',
      { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' }
    );
  } catch {
    return;
  }
  try {
    await deleteCompany(row.id);
    ElMessage.success('公司已删除');
    await loadCompanies();
  } catch {
    /* 错误已由拦截器提示 */
  }
}

function enterCompany(row: CompanyVO): void {
  router.push(`/classes/${row.id}`);
}

onMounted(loadCompanies);
</script>

<template>
  <div v-loading="loading">
    <div class="page-header">
      <h2 class="page-title">班级管理 · 公司</h2>
      <el-button type="primary" :icon="undefined" @click="openCreate">新增公司</el-button>
    </div>

    <el-row :gutter="16">
      <el-col v-for="c in companies" :key="c.id" :xs="24" :sm="12" :md="8" :lg="6">
        <el-card class="company-card" shadow="hover" @click="enterCompany(c)">
          <div class="card-body">
            <div class="company-name">{{ c.name }}</div>
            <div class="company-meta">{{ c.classCount }} 个班级</div>
          </div>
          <div class="card-actions" @click.stop>
            <el-button link type="primary" size="small" @click="openRename(c)">改名</el-button>
            <el-button link type="danger" size="small" @click="handleDelete(c)">删除</el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="400px">
      <el-input
        v-model="formName"
        placeholder="请输入公司名称，如：茶园、果园"
        maxlength="20"
        show-word-limit
        @keyup.enter="submitForm"
      />
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitForm">确定</el-button>
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
}
.page-title {
  margin: 0;
  font-size: 18px;
}
.company-card {
  margin-bottom: 16px;
  cursor: pointer;
  .card-body {
    padding: 12px 4px 16px;
    text-align: center;
  }
  .company-name {
    font-size: 18px;
    font-weight: 600;
    color: #303133;
    margin-bottom: 8px;
  }
  .company-meta {
    font-size: 13px;
    color: #909399;
  }
  .card-actions {
    text-align: right;
    border-top: 1px solid #ebeef5;
    padding-top: 8px;
  }
}
</style>
