<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth.js'
import { useChatStore } from '../stores/chat.js'
import Sidebar from '../components/Sidebar.vue'
import ChatPanel from '../components/ChatPanel.vue'
import { SwitchButton } from '@element-plus/icons-vue'

const router = useRouter()
const authStore = useAuthStore()
const chatStore = useChatStore()
const loadError = ref('')

onMounted(async () => {
  try {
    await chatStore.fetchConversations()
  } catch (err) {
    console.error('加载会话列表失败:', err)
    if (err.message?.includes('鉴权失败')) {
      authStore.logout()
      router.push('/login')
    }
  }
})

function handleLogout() {
  authStore.logout()
  router.push('/login')
}
</script>

<template>
  <div class="chat-page">
    <Sidebar />

    <div class="main-area">
      <div class="top-bar">
        <span class="username">{{ authStore.username }}</span>
        <el-button text size="small" @click="handleLogout">
          <el-icon><SwitchButton /></el-icon>
          退出
        </el-button>
      </div>

      <ChatPanel />
    </div>
  </div>
</template>

<style scoped>
.chat-page {
  height: 100vh;
  display: flex;
  overflow: hidden;
}

.main-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.top-bar {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 12px;
  padding: 0 24px;
  height: 48px;
  background: var(--bg-white);
  border-bottom: 1px solid var(--border);
  flex-shrink: 0;
}

.username {
  font-size: 13px;
  color: var(--text-gray);
}
</style>
