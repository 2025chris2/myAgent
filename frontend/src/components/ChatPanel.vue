<script setup>
import MessageList from './MessageList.vue'
import InputArea from './InputArea.vue'
import { useChatStore } from '../stores/chat.js'

const chatStore = useChatStore()
</script>

<template>
  <div class="chat-panel">
    <div class="panel-header">
      <el-radio-group v-model="chatStore.chatMode" size="default">
        <el-radio-button value="planapp">寻觅聊天</el-radio-button>
        <el-radio-button value="agent">Super Agent</el-radio-button>
      </el-radio-group>
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

</style>
