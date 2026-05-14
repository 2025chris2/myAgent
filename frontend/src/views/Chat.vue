<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth.js'
import { useChatStore } from '../stores/chat.js'
import Sidebar from '../components/Sidebar.vue'
import ChatPanel from '../components/ChatPanel.vue'
import { SwitchButton, Document } from '@element-plus/icons-vue'

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
        <el-button text size="small" @click="router.push('/introduction')">
          <el-icon><Document /></el-icon>
          Introduction
        </el-button>
        <el-button text size="small" @click="router.push('/capability')">
          <el-icon><Document /></el-icon>
          Capability
        </el-button>

        <a
          href="https://github.com/2025chris2/myAgent.git"
          target="_blank"
          class="github-link"
          title="GitHub"
        >
          <svg height="18" viewBox="0 0 16 16" width="18" fill="currentColor">
            <path d="M8 0C3.58 0 0 3.58 0 8c0 3.54 2.29 6.53 5.47 7.59.4.07.55-.17.55-.38 0-.19-.01-.82-.01-1.49-2.01.37-2.53-.49-2.69-.94-.09-.23-.48-.94-.82-1.13-.28-.15-.68-.52-.01-.53.63-.01 1.08.58 1.23.82.72 1.21 1.87.87 2.33.66.07-.52.28-.87.51-1.07-1.78-.2-3.64-.89-3.64-3.95 0-.87.31-1.59.82-2.15-.08-.2-.36-1.02.08-2.12 0 0 .67-.21 2.2.82.64-.18 1.32-.27 2-.27.68 0 1.36.09 2 .27 1.53-1.04 2.2-.82 2.2-.82.44 1.1.16 1.92.08 2.12.51.56.82 1.27.82 2.15 0 3.07-1.87 3.75-3.65 3.95.29.25.54.73.54 1.48 0 1.07-.01 1.93-.01 2.2 0 .21.15.46.55.38A8.013 8.013 0 0016 8c0-4.42-3.58-8-8-8z"/>
          </svg>
        </a>
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
  justify-content: space-evenly;
  gap: 12px;
  padding: 0 24px;
  height: 48px;
  background: var(--bg-white);
  border-bottom: 1px solid var(--border);
  flex-shrink: 0;
}

.top-bar .el-button {
  color: var(--primary, #8B7EC8);
  font-size: 13px;
  gap: 4px;
}

.top-bar .el-button:hover {
  color: var(--primary-dark, #6B5FA0);
  background: var(--primary-light, #E8E0F0);
}

.github-link {
  display: flex;
  align-items: center;
  color: var(--text-gray);
  transition: color 0.2s;
}

.github-link:hover {
  color: var(--text-dark);
}

.username {
  font-size: 13px;
  color: var(--text-gray);
}
</style>
