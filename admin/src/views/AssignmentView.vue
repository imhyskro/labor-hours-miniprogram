<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue';
import { ElMessage } from 'element-plus';
import {
  getUnassignedAssistants,
  assignAssistant,
  unassignAssistant,
} from '@/api/assistants';
import { getMasterList, type MasterListViewVO } from '@/api/masterList';
import { getClassList, type ClassVO } from '@/api/classes';

const loading = ref(false);
const submitLoading = ref(false);

// 显示所有助教（含已分配），按未分配优先排序（后端 selectUnassignedAssistants
// 只返回未分配；为了在一个页面内完成分配 + 取消，这里改为查询全部助教列表）
const tableData = ref<MasterListViewVO[]>([]);
const classOptions = ref<ClassVO[]>([]);

// 每行临时选择：studentId -> classId
const rowSelectedClassMap = reactive<Record<number, number | null>>({});

async function loadAllAssistants() {
  loading.value = true;
  try {
    // 取所有助教：通过总表 identity=ASSISTANT
    const res = await getMasterList({
      page: 1,
      size: 1000,
      identity: 'ASSISTANT',
    });
    if (res.code === 200) {
      // 排序：未分配优先（assignedClassId 为空）
      const list = res.data.records;
      list.sort((a, b) => {
        const aUn = a.assignedClassId == null ? 0 : 1;
        const bUn = b.assignedClassId == null ? 0 : 1;
        return aUn - bUn;
      });
      tableData.value = list;
      list.forEach((r) => {
        rowSelectedClassMap[r.id] = r.assignedClassId ?? null;
      });
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

async function handleAssign(row: MasterListViewVO) {
  const classId = rowSelectedClassMap[row.id];
  if (classId == null) {
    ElMessage.warning('请先选择班级');
    return;
  }
  submitLoading.value = true;
  try {
    const res = await assignAssistant(row.id, classId);
    if (res.code === 200) {
      ElMessage.success('分配成功');
      await loadAllAssistants();
    } else {
      ElMessage.error(res.message || '分配失败');
    }
  } catch (e) {
    ElMessage.error((e as Error).message || '分配失败');
  } finally {
    submitLoading.value = false;
  }
}

async function handleUnassign(row: MasterListViewVO) {
  if (row.assignedClassId == null) {
    ElMessage.warning('该助教尚未分配班级');
    return;
  }
  submitLoading.value = true;
  try {
    const res = await unassignAssistant(row.id);
    if (res.code === 200) {
      ElMessage.success('取消分配成功');
      await loadAllAssistants();
    } else {
      ElMessage.error(res.message || '取消分配失败');
    }
  } catch (e) {
    ElMessage.error((e as Error).message || '取消分配失败');
  } finally {
    submitLoading.value = false;
  }
}

function handleRefresh() {
  loadAllAssistants();
}

onMounted(() => {
  loadClassOptions();
  loadAllAssistants();
});
</script>

<template>
  <div class="assign-container">
    <el-card shadow="never">
      <div class="toolbar">
        <span class="page-title">助教分配</span>
        <el-button @click="handleRefresh" :loading="loading">
          <el-icon><Refresh /></el-icon>
          刷新
        </el-button>
      </div>

      <el-alert
        title="分配规则"
        type="info"
        :closable="false"
        show-icon
      >
        <ul class="rule-list">
          <li>列表显示所有助教，未分配的排在前面</li>
          <li>一个助教最多负责一个班级；一个班级可分配多个助教（无限制）</li>
          <li>通过下拉选择班级，点击"分配"完成；已分配可点击"取消分配"</li>
        </ul>
      </el-alert>

      <el-table :data="tableData" v-loading="loading" border style="width: 100%; margin-top: 12px">
        <el-table-column type="index" label="序号" width="60" align="center" />
        <el-table-column prop="studentId" label="学号" width="140" />
        <el-table-column prop="name" label="姓名" width="120" />
        <el-table-column prop="className" label="所属班级" min-width="140" />
        <el-table-column label="当前负责班级" min-width="160">
          <template #default="{ row }">
            <el-tag v-if="row.assignedClassName" type="primary" size="small">
              {{ row.assignedClassName }}
            </el-tag>
            <el-tag v-else type="info" size="small">未分配</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="分配操作" width="280">
          <template #default="{ row }">
            <el-select
              v-model="rowSelectedClassMap[row.id]"
              placeholder="选择班级"
              style="width: 180px"
              :disabled="row.assignedClassId != null"
              clearable
            >
              <el-option
                v-for="c in classOptions"
                :key="c.id"
                :label="c.className"
                :value="c.id"
              />
            </el-select>
            <el-button
              v-if="row.assignedClassId == null"
              type="primary"
              size="small"
              :loading="submitLoading"
              style="margin-left: 8px"
              @click="handleAssign(row)"
            >分配</el-button>
          </template>
        </el-table-column>
        <el-table-column label="取消" width="100" align="center">
          <template #default="{ row }">
            <el-button
              v-if="row.assignedClassId != null"
              type="danger"
              size="small"
              link
              :loading="submitLoading"
              @click="handleUnassign(row)"
            >取消分配</el-button>
            <span v-else>-</span>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<style scoped lang="scss">
.assign-container {
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
  .rule-list {
    margin: 6px 0 0;
    padding-left: 18px;
    li {
      line-height: 1.8;
    }
  }
}
</style>
