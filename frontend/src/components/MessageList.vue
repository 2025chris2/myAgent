<script setup>
import { ref, watch, nextTick, computed } from 'vue'
import { useChatStore } from '../stores/chat.js'
import MessageBubble from './MessageBubble.vue'
import { Loading } from '@element-plus/icons-vue'
import { marked } from 'marked'

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

const welcomeHtml = computed(() => {
  if (chatStore.chatMode === 'agent') {
    return marked.parse(`
## 🤖 Super Agent

**Agent 的核心，是让大模型从一个只会聊天的"大脑"，进化成能自主完成任务的"数字员工"。**

这个核心建立在三个互为支撑的支点上：

---

### 🔄 感知 — 行动闭环

它不是被动问答，而是在 **观察 → 决策 → 执行 → 观察反馈** 中自主循环。想再多不如真正调用一次工具去验证和推进。

### 🧠 推理与规划的大脑

面对"帮我研究新能源车"这样的模糊目标，能自己拆解成 **搜政策 → 找头部公司 → 对比财报 → 总结趋势** 等步骤，并在某一步失败时自动修正重试。

### 🛠 使用工具的手脚

能真实调用搜索引擎、代码解释器、外部 API 等，把想法落地为看得见的结果，而不只停留在给建议。

---

这三点之上，真正的灵魂是 **目标导向的自主性** —— 用户只需说"做什么"，完全不用教"怎么做"，一切由它自己想办法完成。

> **一句话总结：Agent = 能规划反思的大脑 + 能实际执行的手脚，在一个自主闭环中持续逼近目标。**
    `.trim())
  }

  // 寻觅聊天
  return marked.parse(`
## 🤖 寻觅聊天

大家好，我是资深旅行定制师 **EternalChristmas**～ 接下来我会根据你提供的信息生成详尽旅行报告。

### 报告规则

- **信息核验**：先确认目的地、出行日期（精确到月份）、天数，只追问缺失项
- **标题格式**：\`{地点}旅行指南 - {月份/日期} {天数}日行程\`
- **内容涵盖**：季节性贴士、每日详细行程（分上下午晚上）、深度推荐（含拍照机位）、美食清单（景区/市区，附店铺、人均、招牌菜）、住宿方案（多价位）、预算参考、注意事项
- **语言风格**：亲切实用，像朋友分享行程

---

### 🛠️ 工具调用

#### \`tool_main:\`（7 个 — Java 主程序）

| 工具 | 功能 |
|------|------|
| FileOperationTool | 读写文件，生成 Markdown |
| PDFGenerationTool | 生成 PDF 文件 |
| TerminalOperationTool | 操作服务器终端（Linux/Windows） |
| TerminateTool | 终止工具调用 |
| WebSearchTool | Tavily 搜索，内容适配 AI |
| WebScrapingTool | 网页内容深度抓取 |
| ResourceDownloadTool | 下载代码、图片等资源 |

#### \`tool_mcp:\`（13 个 — MCP 服务）

- **ImageSearch**（Pexels）— 图片搜索
- **Amap Maps**（高德地图 12 工具）— 周边搜索 / 驾车导航 / 公交规划 / 步行路线 / 骑行路线 / 距离计算 / 地理编码 / IP 定位 / 逆地理编码 / 详情查询 / 文本搜索 / 天气查询

---

需要我先帮你核验已有的旅行信息，确认是否缺失出行日期或天数吗？
  `.trim())
})
</script>

<template>
  <div ref="listRef" class="message-list">
    <div v-if="!hasMessages && !chatStore.streaming" class="welcome">
      <div class="welcome-content markdown-body" v-html="welcomeHtml" />
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
  align-items: center;
  justify-content: center;
  height: 100%;
  padding: 40px 24px;
}

.welcome-content {
  max-width: 720px;
  width: 100%;
  max-height: 100%;
  overflow-y: auto;
  padding: 32px 36px;
  background: var(--bg-white);
  border: 1px solid var(--border);
  border-radius: 16px;
  box-shadow: 0 4px 24px rgba(139, 126, 200, 0.08);
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

<style>
@import '../styles/markdown.css';
</style>
