<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue';
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus';
import {
  getStudentPage,
  createStudent,
  updateStudent,
  deleteStudent,
  type StudentVO,
  type StudentCreateDTO,
  type StudentUpdateDTO,
} from '@/api/students';
import { getClassList, type ClassVO } from '@/api/classes';

const loading = ref(false);
const tableData = ref<StudentVO[]>([]);
const total = ref(0);
const currentPage = ref(1);
const pageSize = ref(10);

// 筛选条件
const searchKeyword = ref('');
const filterClassId = ref<number | undefined>(undefined);

// 班级下拉选项
const classOptions = ref<ClassVO[]>([]);

const dialogVisible = ref(false);
const dialogTitle = ref('新增学生');
const isEdit = ref(false);
const formRef = ref<FormInstance>();
const submitLoading = ref(false);

const form = reactive<StudentCreateDTO & { id?: number; status?: number }>({
  id: undefined,
  studentId: '',
  name: '',
  classId: undefined as unknown as number,
  gender: 1,
  status: 1,
});

const rules: FormRules = {
  studentId: [{ required: true, message: '请输入学号', trigger: 'blur' }],
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  classId: [{ required: true, message: '请选择班级', trigger: 'change' }],
  gender: [{ required: true, message: '请选择性别', trigger: 'change' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }],
};

async function loadClassOptions() {
  try {
    const result = await getClassList();
    if (result.code === 200) {
      classOptions.value = result.data;
    }
  } catch {
    // 忽略
  }
}

async function loadData() {
  loading.value = true;
  try {
    const result = await getStudentPage({
      keyword: searchKeyword.value || undefined,
      classId: filterClassId.value ?? undefined,
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
  filterClassId.value = undefined;
  currentPage.value = 1;
  loadData();
}

function handleAdd() {
  isEdit.value = false;
  dialogTitle.value = '新增学生';
  resetForm();
  dialogVisible.value = true;
}

function handleEdit(row: StudentVO) {
  isEdit.value = true;
  dialogTitle.value = '编辑学生';
  resetForm();
  form.id = row.id;
  form.studentId = row.studentId;
  form.name = row.name;
  form.classId = row.classId;
  form.gender = row.gender;
  form.status = row.status;
  dialogVisible.value = true;
}

function resetForm() {
  form.id = undefined;
  form.studentId = '';
  form.name = '';
  form.classId = undefined as unknown as number;
  form.gender = 1;
  form.status = 1;
}

async function handleSubmit() {
  if (!formRef.value) return;
  await formRef.value.validate(async (valid) => {
    if (!valid) return;
    submitLoading.value = true;
    try {
      if (isEdit.value && form.id) {
        const dto: StudentUpdateDTO = {
          id: form.id,
          studentId: form.studentId,
          name: form.name,
          classId: form.classId,
          gender: form.gender,
          status: form.status ?? 1,
        };
        const result = await updateStudent(form.id, dto);
        if (result.code === 200) {
          ElMessage.success('修改成功');
        } else {
          ElMessage.error(result.message);
          return;
        }
      } else {
        const dto: StudentCreateDTO = {
          studentId: form.studentId,
          name: form.name,
          classId: form.classId,
          gender: form.gender,
        };
        const result = await createStudent(dto);
        if (result.code === 200) {
          ElMessage.success('新增成功');
        } else {
          ElMessage.error(result.message);
          return;
        }
      }
      dialogVisible.value = false;
      await loadData();
      // 班级学生数可能变化，刷新下拉
      await loadClassOptions();
    } catch (e) {
      ElMessage.error((e as Error).message || '操作失败');
    } finally {
      submitLoading.value = false;
    }
  });
}

async function handleDelete(row: StudentVO) {
  try {
    await ElMessageBox.confirm(`确定要删除学生「${row.name}（${row.studentId}）」吗？`, '提示', {
      type: 'warning',
      confirmButtonText: '确定',
      cancelButtonText: '取消',
    });
    const result = await deleteStudent(row.id);
    if (result.code === 200) {
      ElMessage.success('删除成功');
      await loadData();
      await loadClassOptions();
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

function genderText(gender: number): string {
  if (gender === 1) return '男';
  if (gender === 2) return '女';
  return '未知';
}

function genderTagType(gender: number): 'primary' | 'danger' | 'info' {
  if (gender === 1) return 'primary';
  if (gender === 2) return 'danger';
  return 'info';
}

function statusTagType(status: number): 'success' | 'info' {
  return status === 1 ? 'success' : 'info';
}

function statusText(status: number): string {
  return status === 1 ? '在读' : '停用';
}

onMounted(() => {
  loadClassOptions();
  loadData();
});
</script>

<template>
  <div class="students-container">
    <el-card shadow="never">
      <div class="toolbar">
        <span class="page-title">学生管理</span>
        <el-button type="primary" @click="handleAdd">
          <el-icon><Plus /></el-icon>
          新增学生
        </el-button>
      </div>

      <div class="search-bar">
        <el-input
          v-model="searchKeyword"
          placeholder="按姓名/学号搜索"
          clearable
          style="width: 240px"
          @keyup.enter="handleSearch"
          @clear="handleSearch"
        />
        <el-select
          v-model="filterClassId"
          placeholder="按班级筛选"
          clearable
          style="width: 220px"
          @change="handleSearch"
        >
          <el-option
            v-for="item in classOptions"
            :key="item.id"
            :label="item.className"
            :value="item.id"
          />
        </el-select>
        <el-button type="primary" @click="handleSearch">
          <el-icon><Search /></el-icon>
          搜索
        </el-button>
        <el-button @click="handleResetSearch">重置</el-button>
      </div>

      <el-table :data="tableData" v-loading="loading" border style="width: 100%">
        <el-table-column type="index" label="序号" width="60" align="center" />
        <el-table-column prop="studentId" label="学号" width="120" />
        <el-table-column prop="name" label="姓名" width="100" />
        <el-table-column label="性别" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="genderTagType(row.gender)" size="small">
              {{ genderText(row.gender) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="className" label="班级" min-width="160" />
        <el-table-column label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" size="small">
              {{ statusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="170" />
        <el-table-column label="操作" width="160" align="center" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleEdit(row)">编辑</el-button>
            <el-button type="danger" link @click="handleDelete(row)">删除</el-button>
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

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="480px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="学号" prop="studentId">
          <el-input v-model="form.studentId" placeholder="如：2024001" />
        </el-form-item>
        <el-form-item label="姓名" prop="name">
          <el-input v-model="form.name" placeholder="请输入姓名" />
        </el-form-item>
        <el-form-item label="班级" prop="classId">
          <el-select v-model="form.classId" placeholder="请选择班级" style="width: 100%">
            <el-option
              v-for="item in classOptions"
              :key="item.id"
              :label="item.className"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="性别" prop="gender">
          <el-radio-group v-model="form.gender">
            <el-radio :value="1">男</el-radio>
            <el-radio :value="2">女</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="isEdit" label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">在读</el-radio>
            <el-radio :value="0">停用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
.students-container {
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
