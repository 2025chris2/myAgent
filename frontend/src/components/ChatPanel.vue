<script setup>
import { useRouter } from 'vue-router'
import MessageList from './MessageList.vue'
import InputArea from './InputArea.vue'
import { useChatStore } from '../stores/chat.js'
import { Document } from '@element-plus/icons-vue'

const chatStore = useChatStore()
const router = useRouter()
</script>

<template>
  <div class="chat-panel">
    <div class="panel-header">
      <el-radio-group v-model="chatStore.chatMode" size="default">
        <el-radio-button value="planapp">寻觅聊天</el-radio-button>
        <el-radio-button value="agent">Super Agent</el-radio-button>
      </el-radio-group>
    </div>

    <div class="nav-buttons">
      <el-button text size="small" @click="router.push('/introduction')">
        <el-icon><Document /></el-icon>
        Introduction
      </el-button>
      <el-button text size="small" @click="router.push('/capability')">
        <el-icon><Document /></el-icon>
        Capability
      </el-button>
    </div>

    <MessageList />

    <InputArea />
  </div>
</template>

<style scoped>
.chat-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  height: 100%;
  background: var(--bg-page);
  overflow: hidden;
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 12px 24px;
  background: var(--bg-white);
  border-bottom: 1px solid var(--border);
  flex-shrink: 0;
}

.nav-buttons {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
  padding: 8px 24px;
  background: var(--bg-white);
  border-bottom: 1px solid var(--border);
  flex-shrink: 0;
}

.nav-buttons .el-button {
  color: var(--primary, #8B7EC8);
  font-size: 13px;
  gap: 4px;
}

.nav-buttons .el-button:hover {
  color: var(--primary-dark, #6B5FA0);
  background: var(--primary-light, #E8E0F0);
}
</style>
