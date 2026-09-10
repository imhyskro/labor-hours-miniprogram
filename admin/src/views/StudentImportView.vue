<script setup lang="ts">
import { ref } from 'vue';
import { ElMessage } from 'element-plus';
import {
  importStudents,
  downloadStudentTemplate,
  type ImportResultVO,
  type ImportErrorVO,
} from '@/api/import';

const loading = ref(false);
const uploadRef = ref();
const result = ref<ImportResultVO | null>(null);

async function handleDownloadTemplate() {
  try {
    const blob = await downloadStudentTemplate();
    saveBlob(blob, '学生导入模板.xlsx');
  } catch (e) {
    ElMessage.error('模板下载失败');
  }
}

function handleFileSelect(file: File) {
  doImport(file);
  // 阻止 el-upload 默认上传行为
  return false;
}

async function doImport(file: File) {
  loading.value = true;
  result.value = null;
  try {
    const res = await importStudents(file);
    if (res.code === 200) {
      result.value = res.data;
      if (res.data.failCount === 0) {
        ElMessage.success(`导入成功，共 ${res.data.successCount} 条`);
      } else {
        ElMessage.warning(`成功 ${res.data.successCount} 条，失败 ${res.data.failCount} 条`);
      }
    } else {
      ElMessage.error(res.message || '导入失败');
    }
  } catch (e) {
    ElMessage.error((e as Error).message || '导入失败');
  } finally {
    loading.value = false;
    // 清空选取，便于再次选取同名文件
    uploadRef.value?.clearFiles?.();
  }
}

function saveBlob(blob: Blob, fileName: string) {
  const url = window.URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = fileName;
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);
  window.URL.revokeObjectURL(url);
}

function reasonType(reason: string): 'error' | 'info' {
  return reason.includes('已是助教') ? 'info' : 'error';
}
</script>

<template>
  <div class="import-container">
    <el-card shadow="never">
      <div class="toolbar">
        <span class="page-title">学生导入</span>
      </div>

      <el-alert
        title="导入规则"
        type="info"
        :closable="false"
        show-icon
      >
        <ul class="rule-list">
          <li>学号必须不存在（已存在的学号会在错误详情中显示）</li>
          <li>班级名称必须已存在于班级管理中（不存在会报错）</li>
          <li>性别列填"男"或"女"，留空或非预期值按未知处理</li>
          <li>模板第 1 行为表头，请勿修改</li>
        </ul>
      </el-alert>

      <div class="actions">
        <el-button @click="handleDownloadTemplate">
          <el-icon><Download /></el-icon>
          下载模板
        </el-button>
        <el-upload
          ref="uploadRef"
          :auto-upload="false"
          :show-file-list="false"
          accept=".xlsx,.xls"
          :before-upload="handleFileSelect"
        >
          <el-button type="primary" :loading="loading">
            <el-icon><Upload /></el-icon>
            选择 Excel 文件上传
          </el-button>
        </el-upload>
      </div>

      <div v-if="result" class="result-block">
        <el-descriptions :column="3" border size="small">
          <el-descriptions-item label="总行数">{{ result.total }}</el-descriptions-item>
          <el-descriptions-item label="成功">
            <el-tag type="success">{{ result.successCount }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="失败">
            <el-tag :type="result.failCount > 0 ? 'danger' : 'info'">{{ result.failCount }}</el-tag>
          </el-descriptions-item>
        </el-descriptions>

        <div v-if="result.errors.length > 0" class="error-table">
          <div class="error-title">错误详情</div>
          <el-table :data="result.errors" border size="small" style="width: 100%">
            <el-table-column prop="row" label="行号" width="80" align="center" />
            <el-table-column prop="studentId" label="学号" width="160" />
            <el-table-column prop="reason" label="失败原因">
              <template #default="{ row }">
                <el-tag :type="reasonType(row.reason)" size="small" effect="plain">
                  {{ row.reason }}
                </el-tag>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </div>
    </el-card>
  </div>
</template>

<style scoped lang="scss">
.import-container {
  .toolbar {
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
  .actions {
    margin: 20px 0;
    display: flex;
    gap: 12px;
  }
  .result-block {
    margin-top: 8px;
  }
  .error-table {
    margin-top: 16px;
    .error-title {
      font-weight: 600;
      margin-bottom: 8px;
      color: #f56c6c;
    }
  }
}
</style>
