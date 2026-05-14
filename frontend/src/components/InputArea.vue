<script setup>
import { ref } from 'vue'
import { useChatStore } from '../stores/chat.js'
import { Promotion } from '@element-plus/icons-vue'

const chatStore = useChatStore()
const inputValue = ref('')
const sending = ref(false)

async function handleSend() {
  const text = inputValue.value.trim()
  if (!text || sending.value) return

  sending.value = true
  inputValue.value = ''

  try {
    await chatStore.sendMessage(text)
  } catch {
    // error handled in store
  } finally {
    sending.value = false
  }
}

function handleKeydown(e) {
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault()
    handleSend()
  }
}
</script>

<template>
  <div class="input-area">
    <div class="input-wrapper">
      <!-- Sync/Async toggle for PlanApp mode -->
      <div v-if="chatStore.chatMode === 'planapp'" class="sync-toggle">
        <el-radio-group v-model="chatStore.syncMode" size="small">
          <el-radio-button value="async">异步</el-radio-button>
          <el-radio-button value="sync">同步</el-radio-button>
        </el-radio-group>
      </div>

      <el-input
        v-model="inputValue"
        type="textarea"
        :rows="1"
        placeholder="输入消息..."
        class="chat-input"
        :disabled="chatStore.streaming"
        @keydown="handleKeydown"
        resize="none"
      />

      <el-button
        v-if="!chatStore.streaming"
        type="primary"
        :disabled="!inputValue.trim()"
        class="send-btn"
        @click="handleSend"
      >
        <el-icon><Promotion /></el-icon>
      </el-button>

      <el-button
        v-else
        type="danger"
        plain
        class="send-btn"
        @click="chatStore.cancelStream()"
      >
        停止
      </el-button>
    </div>
  </div>
</template>

<style scoped>
.input-area {
  padding: 16px 24px 20px;
  background: var(--bg-white);
  border-top: 1px solid var(--border);
}

.input-wrapper {
  display: flex;
  align-items: center;
  gap: 12px;
  background: var(--bg-page);
  border-radius: 12px;
  padding: 8px 8px 8px 16px;
  border: 1px solid var(--border);
  transition: border-color 0.2s;
}

.input-wrapper:focus-within {
  border-color: var(--primary);
}

.sync-toggle {
  flex-shrink: 0;
}

.chat-input {
  flex: 1;
}

.chat-input :deep(.el-textarea__inner) {
  background: transparent;
  border: none;
  box-shadow: none;
  padding: 4px 0;
  font-size: 14px;
  line-height: 1.6;
  resize: none;
  max-height: 120px;
}

.chat-input :deep(.el-textarea__inner:focus) {
  box-shadow: none;
}

.send-btn {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  flex-shrink: 0;
}
</style>
