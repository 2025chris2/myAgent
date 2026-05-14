<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowLeft } from '@element-plus/icons-vue'
import { marked } from 'marked'

const router = useRouter()

const content = ref('')

const mdContent = `
# 项目总览

一个 AI 智能对话平台，前后端分离架构。

---

## 后端（Java 21 + Spring Boot 3.5.14 + Spring AI 1.1.5）

**核心框架**: Spring Boot + JPA + Security + Flyway + PostgreSQL + Redis + pgvector

### 分层架构

| 层 | 关键文件 | 职责 |
|---|---------|------|
| **Controller** | AuthController, ConversationController, AiController | REST API，SSE 流式推送 |
| **App** | PlanApp.java | 「寻觅聊天」模式，基于 DeepSeek ChatModel + RAG + 工具调用 |
| **Agent** | BaseAgent → ReActAgent → ToolCallAgent → llongManus | 「Super Agent」模式，自研 ReAct 代理框架，手动控制工具调用循环 |
| **Tools** | FileOperationTool, PDFGenerationTool, WebSearchTool, WebScrapingTool, TerminalOperationTool, ResourceDownloadTool, TerminateTool | 7 个 AI 可调用工具 |
| **RAG** | PlanAppRAGCustomAdvisor, PlanAppDocumentLoader 等 | pgvector 向量检索 + 查询重写 |
| **Security** | SecurityConfig, JwtTokenProvider, JwtAuthenticationFilter | JWT 无状态认证，BCrypt 密码加密 |

### 两种聊天模式

- **PlanApp（寻觅聊天）**：旅行定制专家，支持同步/异步 SSE，有预设 System Prompt 和旅行文档 RAG
- **Super Agent**：通用 AI 智能体，ReAct 循环（思考→行动），最多 20 步，SSE 推送 typed events（thinking / tool_call / tool_result / final_answer）

### 数据库

3 张表 — \`users\`（6位随机ID） → \`conversations\`（6位随机ID） → \`messages\`（自增ID，JSON 存储消息）

---

## 前端（Vue 3 + Vite + Element Plus + Pinia）

**路由**: \`/login\`（登录/注册） ↔ \`/chat\`（主聊天页，需 JWT Token）

### 组件树

\`\`\`
App.vue → Chat.vue
            ├── Sidebar.vue（会话列表，新建/删除）
            └── ChatPanel.vue（模式切换 + 消息列表 + 输入框）
                  ├── MessageList.vue（欢迎页 / 消息渲染）
                  │     └── MessageBubble.vue（思考面板 + 内容 + 下载链接）
                  └── InputArea.vue（文本输入 + 同步/异步切换 + 发送/停止）
\`\`\`

### Store 设计

- **auth.js**：Token / 用户名持久化到 localStorage
- **chat.js**：会话管理、消息管理、SSE 流式处理、双模式调度

### 亮点

- 自研 SSE 解析器（streamRequest），兼容 Spring SseEmitter 格式
- 思考面板（think panel）：Agent 模式下展示推理过程（思考 / 工具调用 / 工具结果 / 错误）
- 自动下载：Agent 生成文件后自动触发浏览器下载
- 下载链接渲染：\`/api/files/download?file=...\` 自动转为可点击链接
`

onMounted(() => {
  content.value = marked(mdContent)
})
</script>

<template>
  <div class="intro-page">
    <div class="intro-header">
      <el-button text @click="router.push('/chat')">
        <el-icon><ArrowLeft /></el-icon>
        返回聊天
      </el-button>
      <span class="header-title">项目总览</span>
      <span class="header-spacer"></span>
    </div>

    <div class="intro-content">
      <div class="markdown-body" v-html="content"></div>
    </div>
  </div>
</template>

<style scoped>
.intro-page {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: var(--bg-white);
}

.intro-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  height: 56px;
  background: var(--bg-white);
  border-bottom: 1px solid var(--border);
  flex-shrink: 0;
}

.header-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-dark);
}

.header-spacer {
  width: 80px;
}

.intro-content {
  flex: 1;
  overflow-y: auto;
  padding: 32px 48px;
  max-width: 860px;
  margin: 0 auto;
  width: 100%;
}
</style>

<style>
@import '../styles/markdown.css';
</style>
