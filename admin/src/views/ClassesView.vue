<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue';
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus';
import {
  getClassPage,
  createClass,
  updateClass,
  deleteClass,
  type ClassVO,
  type ClassCreateDTO,
  type ClassUpdateDTO,
} from '@/api/classes';

const loading = ref(false);
const tableData = ref<ClassVO[]>([]);
const total = ref(0);
const currentPage = ref(1);
const pageSize = ref(10);

const dialogVisible = ref(false);
const dialogTitle = ref('新增班级');
const isEdit = ref(false);
const formRef = ref<FormInstance>();
const submitLoading = ref(false);

const form = reactive<ClassCreateDTO & { id?: number; status?: number }>({
  id: undefined,
  className: '',
  classCode: '',
  academicYear: '',
  status: 1,
});

const rules: FormRules = {
  className: [{ required: true, message: '请输入班级名称', trigger: 'blur' }],
  classCode: [{ required: true, message: '请输入班级节次', trigger: 'blur' }],
  academicYear: [{ required: true, message: '请输入学年', trigger: 'blur' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }],
};

async function loadData() {
  loading.value = true;
  try {
    const result = await getClassPage(currentPage.value, pageSize.value);
    if (result.code === 200) {
      tableData.value = result.data.records;
      total.value = result.data.total;
    }
  } catch (e) {
    ElMessage.error('查询失败');
  } finally {
    loading.value = false;
  }
}

function handleAdd() {
  isEdit.value = false;
  dialogTitle.value = '新增班级';
  resetForm();
  dialogVisible.value = true;
}

function handleEdit(row: ClassVO) {
  isEdit.value = true;
  dialogTitle.value = '编辑班级';
  resetForm();
  form.id = row.id;
  form.className = row.className;
  form.classCode = row.classCode;
  form.academicYear = row.academicYear;
  form.status = row.status;
  dialogVisible.value = true;
}

function resetForm() {
  form.id = undefined;
  form.className = '';
  form.classCode = '';
  form.academicYear = '';
  form.status = 1;
}

async function handleSubmit() {
  if (!formRef.value) return;
  await formRef.value.validate(async (valid) => {
    if (!valid) return;
    submitLoading.value = true;
    try {
      if (isEdit.value && form.id) {
        const dto: ClassUpdateDTO = {
          id: form.id,
          className: form.className,
          classCode: form.classCode,
          academicYear: form.academicYear,
          status: form.status ?? 1,
        };
        const result = await updateClass(form.id, dto);
        if (result.code === 200) {
          ElMessage.success('修改成功');
        } else {
          ElMessage.error(result.message);
          return;
        }
      } else {
        const dto: ClassCreateDTO = {
          className: form.className,
          classCode: form.classCode,
          academicYear: form.academicYear,
        };
        const result = await createClass(dto);
        if (result.code === 200) {
          ElMessage.success('新增成功');
        } else {
          ElMessage.error(result.message);
          return;
        }
      }
      dialogVisible.value = false;
      await loadData();
    } catch (e) {
      ElMessage.error((e as Error).message || '操作失败');
    } finally {
      submitLoading.value = false;
    }
  });
}

async function handleDelete(row: ClassVO) {
  try {
    await ElMessageBox.confirm(`确定要删除班级「${row.className}」吗？`, '提示', {
      type: 'warning',
      confirmButtonText: '确定',
      cancelButtonText: '取消',
    });
    const result = await deleteClass(row.id);
    if (result.code === 200) {
      ElMessage.success('删除成功');
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

function statusTagType(status: number): 'success' | 'info' {
  return status === 1 ? 'success' : 'info';
}

function statusText(status: number): string {
  return status === 1 ? '启用' : '停用';
}

onMounted(() => {
  loadData();
});
</script>

<template>
  <div class="classes-container">
    <el-card shadow="never">
      <div class="toolbar">
        <span class="page-title">班级管理</span>
        <el-button type="primary" @click="handleAdd">
          <el-icon><Plus /></el-icon>
          新增班级
        </el-button>
      </div>

      <el-table :data="tableData" v-loading="loading" border style="width: 100%">
        <el-table-column type="index" label="序号" width="60" align="center" />
        <el-table-column prop="className" label="班级名称" min-width="180" />
        <el-table-column prop="classCode" label="班级节次" width="140" />
        <el-table-column prop="academicYear" label="学年" width="120" />
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="studentCount" label="学生数" width="100" align="center" />
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
        <el-form-item label="班级名称" prop="className">
          <el-input v-model="form.className" placeholder="如：2024级计算机1班" />
        </el-form-item>
        <el-form-item label="班级节次" prop="classCode">
          <el-input v-model="form.classCode" placeholder="请输入班级节次，如：1-1-2-9" />
          <div class="form-help-text">格式：周次-开始节次-结束节次-班内编号，示例：1-1-2-9 表示第1周12节第9号学生</div>
        </el-form-item>
        <el-form-item label="学年" prop="academicYear">
          <el-input v-model="form.academicYear" placeholder="如：2024-2025" />
        </el-form-item>
        <el-form-item v-if="isEdit" label="状态" prop="status">
          <el-select v-model="form.status" placeholder="请选择状态" style="width: 100%">
            <el-option label="启用" :value="1" />
            <el-option label="停用" :value="0" />
          </el-select>
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
.classes-container {
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
  .pagination-wrapper {
    display: flex;
    justify-content: flex-end;
    margin-top: 16px;
  }
}
.form-help-text {
  font-size: 12px;
  color: #909399;
  line-height: 1.4;
  margin-top: 4px;
}
</style>
