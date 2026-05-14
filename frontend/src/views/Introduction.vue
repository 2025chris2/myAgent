<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowLeft } from '@element-plus/icons-vue'
import { marked } from 'marked'

const router = useRouter()

const content = ref('')

const mdContent = `
# 产品介绍

---

## 一、寻觅 App

寻觅是一款智能旅行助手应用，专为旅行爱好者和选择困难的用户打造。

### 1. RAG（检索增强生成）

服务器上内置了两类关于旅行的文档：

- **旅行推荐与规划**：涵盖去哪旅行、旅行推荐、旅行前/旅行时/旅行后的安排，专为选择困难和规划困难的用户准备
- **旅行相关问题库**：包含 **300+** 个热点问题，帮助用户提前应对各种状况，或在遇到问题时提供正确的抉择

### 2. MCP（图片搜索服务）

小伙伴 **llong** 开发了一个图片搜索的 MCP 服务，本地部署，可以搜索用户想要搜索的内容。

### 3. Tool Call（工具调用）

小伙伴开发了 **7** 个工具，让 AI 具备强大的实际操作能力：

| 工具名称 | 功能描述 |
|---------|---------|
| **FileOperationTool** | 包含读和写文件操作，主要用于生成 Markdown 文件 |
| **PDFGenerationTool** | 生成 PDF 文件 |
| **TerminalOperationTool** | 终端操作工具，让 AI 可以操作服务器终端，适配 Linux 和 Windows |
| **TerminateTool** | 让 AI 终止工具调用的工具 |
| **WebSearchTool** | 使用 Tavily 搜索，内容更适配 AI，效果更好，速度更快 |
| **WebScrapingTool** | 抓取网页内容，当搜索到符合的内容时进行深度解读，获取更完整的内容 |
| **ResourceDownloadTool** | 源码下载工具，可下载代码、图片等，只要包含可访问的 URL 即可下载 |

### 4. Vector（向量检索）

除了 RAG 的向量检索外，当用户提问时，向量数据库会做相似度检索，取 **topK=3** 的相关文档片段，确保回答的准确性和相关性。

### 5. 记忆机制

寻觅 App 采用**滑动窗口记忆**，存储最近的 **20** 条信息，保持上下文窗口的精简高效。每个对话使用 **conversationID** 来区分，所有对话都会持久化存储到 **PostgreSQL** 数据库中。

---

## 二、Super Agent（llongAgent）

Super Agent 是 llong 开发的智能代理系统：

- 采用**滑动窗口记忆**机制，每个 Agent 都拥有独立的记忆空间
- 每次对话**不会**持久化存储到 PostgreSQL 数据库中，保证对话的隐私性和轻量化
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
      <span class="header-title">产品介绍</span>
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
