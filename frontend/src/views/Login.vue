<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth.js'
import { User, Lock } from '@element-plus/icons-vue'

const router = useRouter()
const authStore = useAuthStore()

const activeTab = ref('login')
const form = ref({ username: '', password: '' })
const loading = ref(false)
const errorMsg = ref('')

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 50, message: '用户名 3~50 字符', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码至少 6 位', trigger: 'blur' },
  ],
}

const formRef = ref(null)

async function handleSubmit() {
  if (loading.value) return

  const valid = await formRef.value?.validate().catch((err) => {
    console.error('表单验证异常:', err)
    return false
  })
  if (!valid) return

  loading.value = true
  errorMsg.value = ''
  try {
    if (activeTab.value === 'login') {
      await authStore.login(form.value.username, form.value.password)
    } else {
      await authStore.register(form.value.username, form.value.password)
    }
    await router.push('/chat')
  } catch (err) {
    console.error('登录/注册失败:', err)
    errorMsg.value = err.message || '操作失败，请检查网络连接'
  } finally {
    loading.value = false
  }
}

function handleTabChange() {
  errorMsg.value = ''
  formRef.value?.resetFields()
}
</script>

<template>
  <div class="login-page">
    <div class="login-card">
      <div class="card-header">
        <h1>llongAgent</h1>
        <p>AI 智能对话平台</p>
      </div>

      <el-tabs v-model="activeTab" class="login-tabs" @tab-change="handleTabChange">
        <el-tab-pane label="登录" name="login" />
        <el-tab-pane label="注册" name="register" />
      </el-tabs>

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-position="top"
        @submit.prevent="handleSubmit"
      >
        <el-form-item label="用户名" prop="username">
          <el-input
            v-model="form.username"
            placeholder="请输入用户名"
            :prefix-icon="User"
            size="large"
          />
        </el-form-item>

        <el-form-item label="密码" prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="请输入密码"
            :prefix-icon="Lock"
            size="large"
            show-password
            @keyup.enter="handleSubmit"
          />
        </el-form-item>

        <el-alert v-if="errorMsg" :title="errorMsg" type="error" show-icon :closable="false" />

        <el-button
          type="primary"
          size="large"
          :loading="loading"
          class="submit-btn"
          @click="handleSubmit"
        >
          {{ activeTab === 'login' ? '登 录' : '注 册' }}
        </el-button>
      </el-form>
    </div>
  </div>
</template>



<style scoped>
.login-page {
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #E8E0F0 0%, #F5F0FA 40%, #D5C8F0 100%);
  position: relative;
  overflow: hidden;
}

.login-page::before {
  content: '';
  position: absolute;
  width: 500px;
  height: 500px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(139, 126, 200, 0.1) 0%, transparent 70%);
  top: -100px;
  right: -100px;
}

.login-page::after {
  content: '';
  position: absolute;
  width: 400px;
  height: 400px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(139, 126, 200, 0.08) 0%, transparent 70%);
  bottom: -80px;
  left: -80px;
}

.login-card {
  width: 420px;
  padding: 48px 40px 36px;
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(20px);
  border-radius: 16px;
  box-shadow: 0 8px 32px rgba(139, 126, 200, 0.15);
  z-index: 1;
}

.card-header {
  text-align: center;
  margin-bottom: 24px;
}

.card-header h1 {
  font-size: 28px;
  color: var(--primary);
  font-weight: 700;
  letter-spacing: -0.5px;
}

.card-header p {
  color: var(--text-gray);
  font-size: 14px;
  margin-top: 6px;
}

.login-tabs {
  margin-bottom: 8px;
}

.login-tabs :deep(.el-tabs__nav-wrap::after) {
  height: 1px;
}

.submit-btn {
  width: 100%;
  margin-top: 8px;
  height: 44px;
  font-size: 15px;
  letter-spacing: 4px;
}

.el-alert {
  margin-bottom: 8px;
}
</style>
