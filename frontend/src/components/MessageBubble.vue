<script setup>
import { ref, nextTick, watch, computed } from 'vue'
import { DOWNLOAD_URL_REGEX, IMAGE_URL_REGEX } from '../utils/download.js'

const props = defineProps({
  message: {
    type: Object,
    required: true,
  },
})

const thinkCollapsed = ref(false)
const thinkBodyRef = ref(null)

const hasThinkLines = computed(() => {
  return props.message.thinkLines && props.message.thinkLines.length > 0
})

function typeIcon(type) {
  switch (type) {
    case 'thinking': return '💭'
    case 'tool_call': return '🔧'
    case 'tool_result': return '📋'
    case 'error': return '❌'
    default: return '•'
  }
}

function escapeHtml(text) {
  if (!text) return ''
  return String(text)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
}

function renderContent(text) {
  if (!text) return ''

  // 合并正则：下载链接 + 外部图片链接，单遍扫描
  const fullRegex = new RegExp(
    DOWNLOAD_URL_REGEX.source + '|' + IMAGE_URL_REGEX.source,
    'gi',
  )

  let result = ''
  let lastIdx = 0
  let match
  while ((match = fullRegex.exec(text)) !== null) {
    const rawMatch = match[0]
    result += escapeHtml(text.substring(lastIdx, match.index))

    if (rawMatch.startsWith('/api/files/download') || rawMatch.startsWith('/tmp/')) {
      // 下载链接
      let downloadUrl, fileName
      if (match[2]) {
        downloadUrl = match[1]
        fileName = decodeURIComponent(match[2].split('/').pop())
      } else {
        const fullPath = match[3]
        const relativePath = fullPath.substring(fullPath.indexOf('/tmp/') + 5)
        downloadUrl = '/api/files/download?file=' + encodeURIComponent(relativePath)
        fileName = relativePath.split('/').pop()
      }
      result += `<a href="${escapeHtml(downloadUrl)}" class="file-download-link" target="_blank">📥 ${escapeHtml(fileName)}</a>`
    } else {
      // 外部图片链接 → 渲染为 img 标签
      result += `<img src="${escapeHtml(rawMatch)}" class="inline-image" loading="lazy" alt="图片" />`
    }

    lastIdx = fullRegex.lastIndex
  }
  result += escapeHtml(text.substring(lastIdx))
  return result.replace(/\n/g, '<br>')
}

// Auto-scroll think panel when new lines arrive
watch(
  () => props.message.thinkLines?.length,
  () => {
    nextTick(() => {
      if (thinkBodyRef.value) {
        thinkBodyRef.value.scrollTop = thinkBodyRef.value.scrollHeight
      }
    })
  },
)
</script>

<template>
  <div class="message-row" :class="message.role">
    <div class="avatar">
      <span v-if="message.role === 'user'">我</span>
      <span v-else>AI</span>
    </div>
    <div class="bubble">
      <!-- Think panel for agent mode -->
      <div v-if="hasThinkLines && message.role === 'assistant'" class="think-panel">
        <div class="think-header" @click="thinkCollapsed = !thinkCollapsed">
          <span>🧠 思考过程</span>
          <span class="toggle-icon">{{ thinkCollapsed ? '▶' : '▼' }}</span>
        </div>
        <div v-show="!thinkCollapsed" ref="thinkBodyRef" class="think-body">
          <div
            v-for="(line, index) in message.thinkLines"
            :key="index"
            :class="['think-line', line.type]"
          >
            <span class="think-icon">{{ typeIcon(line.type) }}</span>
            <span class="think-text" v-html="renderContent(line.data)" />
          </div>
        </div>
      </div>

      <!-- Answer content -->
      <div v-if="message.content" class="content" v-html="renderContent(message.content)" />
      <div class="time">{{ message.time }}</div>
    </div>
  </div>
</template>

<style scoped>
.message-row {
  display: flex;
  gap: 12px;
  padding: 16px 0;
  max-width: 85%;
}

.message-row.user {
  flex-direction: row-reverse;
  align-self: flex-end;
  margin-left: auto;
}

.message-row.assistant {
  align-self: flex-start;
}

.avatar {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 600;
  flex-shrink: 0;
}

.user .avatar {
  background: var(--primary);
  color: #fff;
}

.assistant .avatar {
  background: var(--primary-light);
  color: var(--primary-dark);
}

.bubble {
  display: flex;
  flex-direction: column;
  gap: 8px;
  min-width: 0;
}

/* ===== Think Panel ===== */
.think-panel {
  border: 1px solid var(--border);
  border-radius: 10px;
  overflow: hidden;
  background: #FAF8FD;
}

.think-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 14px;
  font-size: 13px;
  color: var(--primary-dark);
  cursor: pointer;
  user-select: none;
  background: #F2ECF8;
}

.think-header:hover {
  background: #E8E0F0;
}

.toggle-icon {
  font-size: 10px;
  color: var(--text-gray);
}

.think-body {
  max-height: 240px;
  overflow-y: auto;
  padding: 8px 14px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.think-line {
  display: flex;
  gap: 6px;
  font-size: 12px;
  line-height: 1.5;
}

.think-icon {
  flex-shrink: 0;
  font-size: 12px;
}

.think-text {
  color: var(--text-light);
  word-break: break-word;
}

.think-line.thinking .think-text {
  color: var(--text-dark);
}

.think-line.tool_call .think-text {
  color: var(--primary);
}

.think-line.tool_result .think-text {
  color: #6B8F5E;
}

.think-line.error .think-text {
  color: #D9534F;
}

/* ===== Content ===== */
.content {
  padding: 12px 16px;
  border-radius: 12px;
  font-size: 14px;
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-word;
}

.user .content {
  background: var(--bubble-user);
  color: var(--bubble-user-text);
  border-bottom-right-radius: 4px;
}

.assistant .content {
  background: var(--bubble-ai);
  border: 1px solid var(--bubble-ai-border);
  border-bottom-left-radius: 4px;
}

.time {
  font-size: 11px;
  color: var(--text-gray);
  padding: 0 4px;
}

:deep(.file-download-link) {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 3px 10px;
  margin: 2px 4px;
  background: var(--primary-light, #E8F0FE);
  color: var(--primary, #4A6CF7);
  border-radius: 6px;
  font-size: 13px;
  text-decoration: none;
  white-space: nowrap;
}
:deep(.file-download-link:hover) {
  background: var(--primary, #4A6CF7);
  color: #fff;
}

:deep(.inline-image) {
  display: block;
  max-width: 100%;
  max-height: 320px;
  border-radius: 10px;
  margin: 8px 0;
  border: 1px solid var(--border);
  cursor: pointer;
  transition: transform 0.15s;
}
:deep(.inline-image:hover) {
  transform: scale(1.02);
}

.user .time {
  text-align: right;
}
</style>
