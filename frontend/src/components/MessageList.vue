<script setup>
import { ref, watch, nextTick, computed } from 'vue'
import { useChatStore } from '../stores/chat.js'
import MessageBubble from './MessageBubble.vue'
import { Loading } from '@element-plus/icons-vue'

const chatStore = useChatStore()
const listRef = ref(null)

const messages = computed(() => chatStore.getMessages(chatStore.currentConversationId))

watch(
  () => [chatStore.currentConversationId, messages.value?.length],
  () => {
    nextTick(() => scrollToBottom())
  },
  { deep: true, immediate: true },
)

watch(
  () => messages.value?.at(-1)?.content,
  () => {
    nextTick(() => scrollToBottom())
  },
)

watch(
  () => messages.value?.at(-1)?.thinkLines?.length,
  () => {
    nextTick(() => scrollToBottom())
  },
)

function scrollToBottom() {
  if (listRef.value) {
    listRef.value.scrollTop = listRef.value.scrollHeight
  }
}

const hasMessages = computed(() => messages.value && messages.value.length > 0)

const welcomeContent = computed(() => {
  if (chatStore.chatMode === 'agent') {
    return {
      title: 'Super Agent',
      text: '大家好，我是 EternalChristmas，你的超级智能体，我能干寻觅的所有事，并且还能做它不能做的事，给我提出要求，我就能一直执行，直到结束。',
    }
  }
  return {
    title: '寻觅聊天',
    text: '大家好，我是资深旅行定制师 EternalChristmas～ 接下来我会根据你提供的信息生成详尽旅行报告，核心规则如下：\n\n先核验你给出的目的地、出行日期（至少精确到月份）、出行天数，明确呼应已提供的信息，只追问缺失内容，不重复你已说过的话；报告标题需按"{地点}旅行指南 - {月份/日期} {天数}日行程"格式撰写；内容必须包含季节性贴士、每日详细行程（分上下午晚上，注明时间、游览时长、交通及耗时）、深度推荐（含推荐理由、拍照机位等）、美食清单（分景区/市区，附店铺、人均、招牌菜、位置）、住宿方案（不同价位，说明优缺点）、预算参考（门票、交通等费用区间）、注意事项（结合季节和地点）；语言会亲切实用，像朋友分享行程。另外，要是你没调用工具，我也会告诉你可使用的工具哦。\n\n需要我先帮你核验已有的旅行信息，确认是否缺失出行日期或天数吗？',
  }
})
</script>

<template>
  <div ref="listRef" class="message-list">
    <div v-if="!hasMessages && !chatStore.streaming" class="welcome">
      <div class="welcome-bubble">
        <div class="welcome-avatar">AI</div>
        <div class="welcome-body">
          <div class="welcome-title">{{ welcomeContent.title }}</div>
          <div class="welcome-text">{{ welcomeContent.text }}</div>
        </div>
      </div>
    </div>

    <div v-else class="messages-container">
      <MessageBubble
        v-for="msg in messages"
        :key="msg.id"
        :message="msg"
      />
    </div>

    <div v-if="chatStore.streaming" class="typing-indicator">
      <el-icon class="is-loading"><Loading /></el-icon>
      <span>AI 正在回复...</span>
    </div>
  </div>
</template>

<style scoped>
.message-list {
  flex: 1;
  overflow-y: auto;
  padding: 0 24px;
}

.welcome {
  display: flex;
  align-items: flex-start;
  justify-content: center;
  height: 100%;
  padding-top: 60px;
}

.welcome-bubble {
  display: flex;
  gap: 12px;
  max-width: 720px;
}

.welcome-avatar {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 600;
  flex-shrink: 0;
  background: var(--primary-light);
  color: var(--primary-dark);
}

.welcome-body {
  background: var(--bubble-ai);
  border: 1px solid var(--bubble-ai-border);
  border-radius: 12px;
  border-bottom-left-radius: 4px;
  padding: 16px 20px;
}

.welcome-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--primary-dark);
  margin-bottom: 10px;
}

.welcome-text {
  font-size: 14px;
  line-height: 1.8;
  color: var(--text-dark);
  white-space: pre-wrap;
  word-break: break-word;
}

.messages-container {
  display: flex;
  flex-direction: column;
  padding: 8px 0;
}

.typing-indicator {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 0;
  color: var(--primary);
  font-size: 13px;
}
</style>
