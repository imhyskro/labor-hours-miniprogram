<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { getMasterList, type MasterListViewVO } from '@/api/masterList';
import { getClassList, type ClassVO } from '@/api/classes';
import { getCompanyList, type CompanyVO } from '@/api/companies';
import { updateStudent, deleteStudent } from '@/api/students';

const loading = ref(false);
const tableData = ref<MasterListViewVO[]>([]);
const total = ref(0);
const currentPage = ref(1);
const pageSize = ref(10);

const classOptions = ref<ClassVO[]>([]);
const companyOptions = ref<CompanyVO[]>([]);

const query = reactive<{ keyword: string; companyId?: number; classId?: number; identity?: string }>({
  keyword: '',
  companyId: undefined,
  classId: undefined,
  identity: undefined,
});

const identityOptions = [
  { label: '全部', value: '' },
  { label: '学生', value: 'STUDENT' },
  { label: '助教', value: 'ASSISTANT' },
];

/** 班级下拉按公司分组 */
const classGroups = computed(() => {
  const map = new Map<string, ClassVO[]>();
  classOptions.value.forEach((c) => {
    const key = c.companyName ?? '未分组';
    if (!map.has(key)) map.set(key, []);
    map.get(key)!.push(c);
  });
  return Array.from(map.entries()).map(([company, list]) => ({ company, list }));
});

async function loadData() {
  loading.value = true;
  try {
    const res = await getMasterList({
      page: currentPage.value,
      size: pageSize.value,
      keyword: query.keyword || undefined,
      companyId: query.companyId ?? undefined,
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

async function loadOptions() {
  try {
    const [cRes, compRes] = await Promise.all([getClassList(), getCompanyList()]);
    if (cRes.code === 200) classOptions.value = cRes.data;
    if (compRes.code === 200) companyOptions.value = compRes.data;
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
  query.companyId = undefined;
  query.classId = undefined;
  query.identity = undefined;
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

// ===== 单条编辑 =====
const editVisible = ref(false);
const editSaving = ref(false);
const editForm = reactive({
  id: 0,
  studentId: '',
  name: '',
  classId: undefined as number | undefined,
  studentNoInClass: null as number | null,
  originalMajor: '',
  gender: 0,
  status: 1,
});

function openEdit(row: MasterListViewVO) {
  editForm.id = row.id;
  editForm.studentId = row.studentId;
  editForm.name = row.name;
  editForm.classId = row.classId ?? undefined;
  editForm.studentNoInClass = row.studentNoInClass;
  editForm.originalMajor = row.originalMajor ?? '';
  editForm.gender = row.gender ?? 0;
  editForm.status = 1;
  editVisible.value = true;
}

async function submitEdit() {
  if (!editForm.studentId.trim() || !editForm.name.trim()) {
    ElMessage.warning('学号和姓名不能为空');
    return;
  }
  const classId = editForm.classId;
  if (!classId) {
    ElMessage.warning('请选择班级');
    return;
  }
  editSaving.value = true;
  try {
    const res = await updateStudent(editForm.id, {
      id: editForm.id,
      studentId: editForm.studentId.trim(),
      name: editForm.name.trim(),
      classId,
      studentNoInClass: editForm.studentNoInClass,
      originalMajor: editForm.originalMajor.trim() || null,
      gender: editForm.gender,
      status: editForm.status,
    });
    if (res.code === 200) {
      ElMessage.success('修改成功');
      editVisible.value = false;
      loadData();
    } else {
      ElMessage.error(res.message || '修改失败');
    }
  } catch (e) {
    ElMessage.error((e as Error).message || '修改失败');
  } finally {
    editSaving.value = false;
  }
}

async function handleDelete(row: MasterListViewVO) {
  try {
    await ElMessageBox.confirm(
      `确定删除学生「${row.name}（${row.studentId}）」吗？${row.isAssistant === 1 ? '该生为助教，将同时停用其账号。' : ''}`,
      '删除确认',
      { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' }
    );
  } catch {
    return;
  }
  try {
    const res = await deleteStudent(row.id);
    if (res.code === 200) {
      ElMessage.success('删除成功');
      loadData();
    } else {
      ElMessage.error(res.message || '删除失败');
    }
  } catch (e) {
    ElMessage.error((e as Error).message || '删除失败');
  }
}

onMounted(() => {
  loadOptions();
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
            style="width: 180px"
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item label="公司">
          <el-select v-model="query.companyId" placeholder="全部" clearable style="width: 140px">
            <el-option v-for="c in companyOptions" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="班级">
          <el-select v-model="query.classId" placeholder="全部" clearable filterable style="width: 200px">
            <el-option-group v-for="g in classGroups" :key="g.company" :label="g.company">
              <el-option
                v-for="c in g.list"
                :key="c.id"
                :label="`${c.classCode ?? ''}（${c.startSession}~${c.endSession}节）`"
                :value="c.id"
              />
            </el-option-group>
          </el-select>
        </el-form-item>
        <el-form-item label="身份">
          <el-select v-model="query.identity" placeholder="全部" clearable style="width: 110px">
            <el-option v-for="o in identityOptions" :key="o.value" :label="o.label" :value="o.value" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="tableData" v-loading="loading" border style="width: 100%">
        <el-table-column prop="studentId" label="学号" width="120" fixed />
        <el-table-column prop="name" label="姓名" width="90" fixed />
        <el-table-column prop="companyName" label="公司" width="90">
          <template #default="{ row }">{{ row.companyName || '—' }}</template>
        </el-table-column>
        <el-table-column label="班级(W-S-E)" width="110" align="center">
          <template #default="{ row }">{{ row.classCode || '—' }}</template>
        </el-table-column>
        <el-table-column prop="studentNoInClass" label="班内编号" width="80" align="center">
          <template #default="{ row }">{{ row.studentNoInClass ?? '—' }}</template>
        </el-table-column>
        <el-table-column prop="fullNo" label="完整编号" width="130" align="center">
          <template #default="{ row }">{{ row.fullNo || '—' }}</template>
        </el-table-column>
        <el-table-column prop="originalMajor" label="原始专业" min-width="140" show-overflow-tooltip>
          <template #default="{ row }">{{ row.originalMajor || '—' }}</template>
        </el-table-column>
        <el-table-column label="性别" width="70" align="center">
          <template #default="{ row }">{{ genderText(row.gender) }}</template>
        </el-table-column>
        <el-table-column label="身份" width="70" align="center">
          <template #default="{ row }">
            <el-tag :type="row.isAssistant === 1 ? 'warning' : 'success'" size="small">
              {{ row.identity }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="助教负责班级" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">
            <span v-if="row.isAssistant === 1 && row.assignedClassNames">{{ row.assignedClassNames }}</span>
            <el-tag v-else-if="row.isAssistant === 1" type="info" size="small">未分配</el-tag>
            <span v-else>—</span>
          </template>
        </el-table-column>
        <el-table-column label="账号" width="70" align="center">
          <template #default="{ row }">
            <el-tag :type="row.hasAccount === 1 ? 'success' : 'info'" size="small">
              {{ row.hasAccount === 1 ? '已建' : '未建' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="130" align="center" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openEdit(row)">编辑</el-button>
            <el-button link type="danger" size="small" @click="handleDelete(row)">删除</el-button>
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

    <!-- 编辑学生弹窗 -->
    <el-dialog v-model="editVisible" title="编辑学生" width="480px">
      <el-form :model="editForm" label-width="90px">
        <el-form-item label="学号">
          <el-input v-model="editForm.studentId" />
        </el-form-item>
        <el-form-item label="姓名">
          <el-input v-model="editForm.name" />
        </el-form-item>
        <el-form-item label="所属班级">
          <el-select v-model="editForm.classId" placeholder="请选择班级" filterable style="width: 100%">
            <el-option-group v-for="g in classGroups" :key="g.company" :label="g.company">
              <el-option
                v-for="c in g.list"
                :key="c.id"
                :label="`${c.classCode ?? ''}（${c.startSession}~${c.endSession}节）`"
                :value="c.id"
              />
            </el-option-group>
          </el-select>
        </el-form-item>
        <el-form-item label="班内编号">
          <el-input-number v-model="editForm.studentNoInClass" :min="1" :max="999" />
        </el-form-item>
        <el-form-item label="原始专业">
          <el-input v-model="editForm.originalMajor" placeholder="如：软件工程" />
        </el-form-item>
        <el-form-item label="性别">
          <el-radio-group v-model="editForm.gender">
            <el-radio :value="1">男</el-radio>
            <el-radio :value="2">女</el-radio>
            <el-radio :value="0">未知</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="editSaving" @click="submitEdit">保存</el-button>
      </template>
    </el-dialog>
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
