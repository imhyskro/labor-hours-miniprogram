<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue';
import { ElMessage } from 'element-plus';
import { getMasterList, type MasterListViewVO } from '@/api/masterList';
import { getClassList, type ClassVO } from '@/api/classes';

const loading = ref(false);
const tableData = ref<MasterListViewVO[]>([]);
const total = ref(0);
const currentPage = ref(1);
const pageSize = ref(10);

const classOptions = ref<ClassVO[]>([]);

const query = reactive<{ keyword: string; classId: number | null; identity: string | null }>({
  keyword: '',
  classId: null,
  identity: null,
});

const identityOptions = [
  { label: '全部', value: '' },
  { label: '学生', value: 'STUDENT' },
  { label: '助教', value: 'ASSISTANT' },
];

async function loadData() {
  loading.value = true;
  try {
    const res = await getMasterList({
      page: currentPage.value,
      size: pageSize.value,
      keyword: query.keyword || undefined,
      classId: query.classId ?? undefined,
      identity: query.identity || undefined,
    });
    if (res.code === 200) {
      tableData.value = res.data.records;
      total.value = res.data.total;
    } else {
      ElMessage.error(res.message || '查询失败');
    }
  } catch (e) {
    ElMessage.error((e as Error).message || '查询失败');
  } finally {
    loading.value = false;
  }
}

async function loadClassOptions() {
  try {
    const res = await getClassList();
    if (res.code === 200) {
      classOptions.value = res.data;
    }
  } catch {
    // 忽略
  }
}

function handleSearch() {
  currentPage.value = 1;
  loadData();
}

function handleReset() {
  query.keyword = '';
  query.classId = null;
  query.identity = null;
  currentPage.value = 1;
  loadData();
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

function genderText(g: number): string {
  return g === 1 ? '男' : g === 2 ? '女' : '未知';
}

function identityTagType(isAssistant: number): 'success' | 'primary' {
  return isAssistant === 1 ? 'primary' : 'success';
}

onMounted(() => {
  loadClassOptions();
  loadData();
});
</script>

<template>
  <div class="master-container">
    <el-card shadow="never">
      <div class="toolbar">
        <span class="page-title">学生总表</span>
      </div>

      <el-form :inline="true" class="filter-form" @submit.prevent>
        <el-form-item label="关键词">
          <el-input
            v-model="query.keyword"
            placeholder="学号 / 姓名"
            clearable
            style="width: 200px"
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item label="所属班级">
          <el-select
            v-model="query.classId"
            placeholder="全部"
            clearable
            style="width: 180px"
          >
            <el-option v-for="c in classOptions" :key="c.id" :label="c.className" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="身份">
          <el-select v-model="query.identity" placeholder="全部" clearable style="width: 120px">
            <el-option
              v-for="o in identityOptions"
              :key="o.value"
              :label="o.label"
              :value="o.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">
            <el-icon><Search /></el-icon>
            搜索
          </el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="tableData" v-loading="loading" border style="width: 100%">
        <el-table-column type="index" label="序号" width="60" align="center" />
        <el-table-column prop="studentId" label="学号" width="140" />
        <el-table-column prop="name" label="姓名" width="120" />
        <el-table-column label="性别" width="80" align="center">
          <template #default="{ row }">{{ genderText(row.gender) }}</template>
        </el-table-column>
        <el-table-column prop="className" label="所属班级" min-width="140" />
        <el-table-column label="身份" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="identityTagType(row.isAssistant)" size="small">
              {{ row.identity }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="助教负责班级" min-width="140">
          <template #default="{ row }">
            <span v-if="row.assignedClassName">{{ row.assignedClassName }}</span>
            <el-tag v-else type="info" size="small">未分配</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="账号" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.hasAccount === 1 ? 'success' : 'info'" size="small">
              {{ row.hasAccount === 1 ? '已建' : '未建' }}
            </el-tag>
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
.master-container {
  .toolbar {
    margin-bottom: 16px;
    .page-title {
      font-size: 18px;
      font-weight: 600;
      color: #303133;
    }
  }
  .filter-form {
    margin-bottom: 8px;
  }
  .pagination-wrapper {
    display: flex;
    justify-content: flex-end;
    margin-top: 16px;
  }
}
</style>
