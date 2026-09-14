<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import {
  getClassesByCompany,
  createClass,
  deleteClass,
  getClassStudents,
  type ClassVO,
  type ClassStudentVO,
} from '@/api/classes';
import { getCompanyList, type CompanyVO } from '@/api/companies';

const route = useRoute();
const router = useRouter();
const companyId = Number(route.params.companyId);

const companyName = ref('');
const classes = ref<ClassVO[]>([]);
const loading = ref(false);

// 新增班级弹窗
const dialogVisible = ref(false);
const form = ref({ week: 1, startSession: 1, endSession: 2 });

// 学生抽屉
const drawerVisible = ref(false);
const drawerTitle = ref('');
const students = ref<ClassStudentVO[]>([]);
const drawerLoading = ref(false);

async function loadDetail(): Promise<void> {
  loading.value = true;
  try {
    const [companyRes, classRes] = await Promise.all([
      getCompanyList(),
      getClassesByCompany(companyId),
    ]);
    const c = (companyRes.data ?? []).find((x: CompanyVO) => x.id === companyId);
    companyName.value = c?.name ?? '公司';
    classes.value = classRes.data ?? [];
  } finally {
    loading.value = false;
  }
}

function openCreate(): void {
  form.value = { week: 1, startSession: 1, endSession: 2 };
  dialogVisible.value = true;
}

async function submitClass(): Promise<void> {
  const { week, startSession, endSession } = form.value;
  if (!week || !startSession || !endSession) {
    ElMessage.warning('请填写周次和节次');
    return;
  }
  if (startSession > endSession) {
    ElMessage.warning('开始节次不能大于结束节次');
    return;
  }
  try {
    await createClass({ companyId, week, startSession, endSession });
    ElMessage.success('班级已创建');
    dialogVisible.value = false;
    await loadDetail();
  } catch {
    /* 错误已由拦截器提示 */
  }
}

async function handleDelete(row: ClassVO): Promise<void> {
  try {
    await ElMessageBox.confirm(
      `确定删除班级「${row.classCode}」吗？班级下存在学生时无法删除。`,
      '删除确认',
      { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' }
    );
  } catch {
    return;
  }
  try {
    await deleteClass(row.id);
    ElMessage.success('班级已删除');
    await loadDetail();
  } catch {
    /* 错误已由拦截器提示 */
  }
}

async function viewStudents(row: ClassVO): Promise<void> {
  drawerTitle.value = `班级 ${row.classCode} 学生名单`;
  drawerVisible.value = true;
  drawerLoading.value = true;
  students.value = [];
  try {
    const res = await getClassStudents(row.id);
    students.value = res.data ?? [];
  } finally {
    drawerLoading.value = false;
  }
}

function genderText(g: number): string {
  return g === 1 ? '男' : g === 2 ? '女' : '未知';
}

onMounted(loadDetail);
</script>

<template>
  <div v-loading="loading">
    <div class="page-header">
      <div class="title-wrap">
        <el-button link @click="router.push('/classes')">← 返回公司列表</el-button>
        <h2 class="page-title">{{ companyName }}</h2>
      </div>
      <el-button type="primary" @click="openCreate">新增班级</el-button>
    </div>

    <el-table :data="classes" border stripe>
      <el-table-column label="周次" prop="week" width="80" align="center" />
      <el-table-column label="开始节次" prop="startSession" width="100" align="center" />
      <el-table-column label="结束节次" prop="endSession" width="100" align="center" />
      <el-table-column label="班级编码 (W-S-E)" prop="classCode" width="160" align="center" />
      <el-table-column label="班级名称" prop="className" min-width="220" />
      <el-table-column label="学生数" prop="studentCount" width="90" align="center" />
      <el-table-column label="操作" width="180" align="center" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click="viewStudents(row)">查看学生</el-button>
          <el-button link type="danger" size="small" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
      <template #empty>
        <el-empty description="该公司暂无班级，点击右上角「新增班级」创建" />
      </template>
    </el-table>

    <!-- 新增班级弹窗 -->
    <el-dialog v-model="dialogVisible" title="新增班级" width="420px">
      <el-form label-width="90px">
        <el-form-item label="周次 W">
          <el-input-number v-model="form.week" :min="1" :max="30" />
        </el-form-item>
        <el-form-item label="开始节次 S">
          <el-input-number v-model="form.startSession" :min="1" :max="20" />
        </el-form-item>
        <el-form-item label="结束节次 E">
          <el-input-number v-model="form.endSession" :min="1" :max="20" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitClass">确定</el-button>
      </template>
    </el-dialog>

    <!-- 学生名单抽屉 -->
    <el-drawer v-model="drawerVisible" :title="drawerTitle" size="60%">
      <el-table v-loading="drawerLoading" :data="students" border stripe>
        <el-table-column label="班内编号" prop="studentNoInClass" width="90" align="center" />
        <el-table-column label="完整编号" prop="fullNo" width="150" align="center" />
        <el-table-column label="学号" prop="studentId" width="140" />
        <el-table-column label="姓名" prop="name" width="120" />
        <el-table-column label="原始专业" prop="originalMajor" min-width="160" />
        <el-table-column label="性别" width="80" align="center">
          <template #default="{ row }">{{ genderText(row.gender) }}</template>
        </el-table-column>
        <el-table-column label="身份" width="90" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.isAssistant === 1" type="warning" size="small">助教</el-tag>
            <span v-else>学生</span>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="该班级暂无学生，请通过「导入学生」添加" />
        </template>
      </el-table>
    </el-drawer>
  </div>
</template>

<style scoped lang="scss">
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}
.title-wrap {
  display: flex;
  align-items: center;
  gap: 12px;
}
.page-title {
  margin: 0;
  font-size: 18px;
}
</style>
