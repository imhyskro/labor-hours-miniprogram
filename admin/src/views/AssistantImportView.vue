<script setup lang="ts">
import { ref } from 'vue';
import { ElMessage } from 'element-plus';
import {
  importAssistants,
  downloadAssistantTemplate,
  type ImportResultVO,
} from '@/api/import';

const loading = ref(false);
const uploadRef = ref();
const result = ref<ImportResultVO | null>(null);

async function handleDownloadTemplate() {
  try {
    const blob = await downloadAssistantTemplate();
    saveBlob(blob, '助教导入模板.xlsx');
  } catch (e) {
    ElMessage.error('模板下载失败');
  }
}

/**
 * 文件选择回调（auto-upload=false 时 before-upload 不触发，
 * 必须用 on-change 捕获文件）
 */
function handleChange(uploadFile: { raw?: File }) {
  if (uploadFile.raw) {
    doImport(uploadFile.raw);
  }
}

async function doImport(file: File) {
  loading.value = true;
  result.value = null;
  try {
    const res = await importAssistants(file);
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

function reasonType(reason: string): 'danger' | 'info' {
  return reason.includes('已是助教') ? 'info' : 'danger';
}
</script>

<template>
  <div class="import-container">
    <el-card shadow="never">
      <div class="toolbar">
        <span class="page-title">助教导入</span>
      </div>

      <el-alert
        title="导入规则"
        type="info"
        :closable="false"
        show-icon
      >
        <ul class="rule-list">
          <li>列顺序：学号 / 姓名 / 公司名称 / 周次 / 开始节次 / 结束节次 / 班内编号 / 原始专业</li>
          <li>公司需已在「班级管理」创建；班级按「公司+周次+节次」自动匹配，不存在自动创建</li>
          <li>学号已存在：标记为助教并开通账号；已是助教则跳过</li>
          <li>学号不存在：自动创建学生记录 + 助教账号</li>
          <li>账号 username=学号，初始密码 cdjcc123456，首次登录强制改密</li>
          <li>助教负责的多个班级请在「助教管理」页面分配</li>
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
          :on-change="handleChange"
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
