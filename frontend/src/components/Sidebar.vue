<script setup>
import { computed } from 'vue'
import { useChatStore } from '../stores/chat.js'
import { Delete, Plus } from '@element-plus/icons-vue'

const chatStore = useChatStore()

const conversations = computed(() =>
  [...chatStore.conversations].sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt)),
)

function handleSelect(id) {
  chatStore.setCurrentConversation(id)
}

function handleDelete(id) {
  chatStore.removeConversation(id)
}

function handleNew() {
  chatStore.createNewConversation()
}
</script>

<template>
  <div class="sidebar">
    <el-button type="primary" class="new-chat-btn" @click="handleNew">
      <el-icon><Plus /></el-icon>
      <span>新对话</span>
    </el-button>

    <div class="conversation-list">
      <div
        v-for="conv in conversations"
        :key="conv.id"
        class="conv-item"
        :class="{ active: conv.id === chatStore.currentConversationId }"
        @click="handleSelect(conv.id)"
      >
        <div class="conv-info">
          <span class="conv-id">{{ conv.id }}</span>
          <span class="conv-time">{{ conv.createdAt?.slice(0, 16)?.replace('T', ' ') }}</span>
        </div>
        <el-button
          text
          size="small"
          class="delete-btn"
          @click.stop="handleDelete(conv.id)"
        >
          <el-icon><Delete /></el-icon>
        </el-button>
      </div>

      <div v-if="conversations.length === 0" class="empty-list">
        暂无对话
      </div>
    </div>
  </div>
</template>

<style scoped>
.sidebar {
  width: 260px;
  height: 100%;
  background: var(--bg-white);
  border-right: 1px solid var(--border);
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
}

.new-chat-btn {
  margin: 16px 16px 12px;
  height: 40px;
  font-size: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
}

.conversation-list {
  flex: 1;
  overflow-y: auto;
  padding: 0 12px 12px;
}

.conv-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 12px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.15s ease;
  margin-bottom: 2px;
}

.conv-item:hover {
  background: var(--primary-light);
}

.conv-item.active {
  background: var(--primary-light);
}

.conv-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
  overflow: hidden;
  flex: 1;
}

.conv-id {
  font-size: 13px;
  color: var(--text-dark);
  font-weight: 500;
  font-family: 'SF Mono', monospace;
}

.conv-time {
  font-size: 11px;
  color: var(--text-gray);
}

.delete-btn {
  opacity: 0;
  color: var(--text-gray);
  transition: opacity 0.15s;
}

.conv-item:hover .delete-btn {
  opacity: 1;
}

.delete-btn:hover {
  color: #e74c3c;
}

.empty-list {
  text-align: center;
  color: var(--text-gray);
  font-size: 13px;
  margin-top: 40px;
}
</style>
