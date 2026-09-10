<script setup lang="ts">
import { ref, reactive, computed } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage, type FormInstance, type FormRules } from 'element-plus';
import { useUserStore } from '@/stores/userStore';
import type { ChangePasswordDTO } from '@/api/auth';

const router = useRouter();
const userStore = useUserStore();

const formRef = ref<FormInstance>();
const loading = ref(false);

const form = reactive<ChangePasswordDTO>({
  oldPassword: '',
  newPassword: '',
  confirmPassword: '',
});

/** 密码复杂度实时校验 */
const upperPattern = /[A-Z]/;
const lowerPattern = /[a-z]/;
const digitPattern = /\d/;
const specialPattern = /[@$!%*?&]/;

const passwordIssues = computed(() => {
  const pwd = form.newPassword;
  if (!pwd) return [];
  const issues: string[] = [];
  if (pwd.length < 8) {
    issues.push('长度不能少于 8 位');
  }
  let category = 0;
  if (upperPattern.test(pwd)) category++;
  if (lowerPattern.test(pwd)) category++;
  if (digitPattern.test(pwd)) category++;
  if (specialPattern.test(pwd)) category++;
  if (category < 3) {
    issues.push('需包含大写字母、小写字母、数字、特殊字符(@$!%*?&)中的至少 3 种');
  }
  return issues;
});

const validateConfirm = (_rule: unknown, value: string, callback: (err?: Error) => void) => {
  if (!value) {
    callback(new Error('请输入确认密码'));
  } else if (value !== form.newPassword) {
    callback(new Error('两次输入的密码不一致'));
  } else {
    callback();
  }
};

const rules: FormRules<ChangePasswordDTO> = {
  oldPassword: [{ required: true, message: '请输入旧密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    {
      validator: (_rule, value, callback) => {
        if (!value) {
          callback(new Error('请输入新密码'));
          return;
        }
        const issues = passwordIssues.value;
        if (issues.length > 0) {
          callback(new Error(issues[0]));
        } else {
          callback();
        }
      },
      trigger: 'blur',
    },
  ],
  confirmPassword: [{ required: true, validator: validateConfirm, trigger: 'blur' }],
};

async function handleSubmit() {
  if (!formRef.value) return;
  await formRef.value.validate(async (valid) => {
    if (!valid) return;
    loading.value = true;
    try {
      await userStore.changePassword({ ...form });
      ElMessage.success('密码修改成功');
      router.push('/dashboard');
    } catch (e) {
      ElMessage.error((e as Error).message || '密码修改失败');
    } finally {
      loading.value = false;
    }
  });
}
</script>

<template>
  <div class="change-password-container">
    <el-card class="change-password-card" shadow="always">
      <template #header>
        <div class="card-title">
          <h3>修改密码</h3>
          <p class="hint">首次登录需修改初始密码</p>
        </div>
      </template>
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="100px"
      >
        <el-form-item label="旧密码" prop="oldPassword">
          <el-input
            v-model="form.oldPassword"
            type="password"
            placeholder="请输入旧密码"
            show-password
          />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input
            v-model="form.newPassword"
            type="password"
            placeholder="请输入新密码"
            show-password
          />
          <div v-if="form.newPassword" class="password-tips">
            <div
              v-for="(issue, idx) in passwordIssues"
              :key="idx"
              class="tip-item tip-error"
            >
              {{ issue }}
            </div>
            <div v-if="passwordIssues.length === 0" class="tip-item tip-success">
              密码强度符合要求
            </div>
          </div>
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input
            v-model="form.confirmPassword"
            type="password"
            placeholder="请再次输入新密码"
            show-password
          />
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            :loading="loading"
            @click="handleSubmit"
          >
            确认修改
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<style scoped lang="scss">
.change-password-container {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.change-password-card {
  width: 460px;
  border-radius: 12px;
}

.card-title {
  text-align: center;
  h3 {
    margin: 0 0 4px;
    color: #303133;
  }
  .hint {
    margin: 0;
    color: #e6a23c;
    font-size: 13px;
  }
}

.password-tips {
  margin-top: 6px;
  font-size: 12px;
  .tip-item {
    line-height: 1.6;
  }
  .tip-error {
    color: #f56c6c;
  }
  .tip-success {
    color: #67c23a;
  }
}
</style>
