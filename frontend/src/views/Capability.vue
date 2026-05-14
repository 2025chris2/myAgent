<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowLeft } from '@element-plus/icons-vue'
import { marked } from 'marked'

const router = useRouter()

const content = ref('')

const mdContent = `
# 能力展示

---

## RAG 检索增强生成

内置丰富的旅行文档知识库，涵盖旅行规划、旅行推荐、旅行前中后安排，以及 **300+** 热点旅行问题。结合向量相似度检索（topK=3），为 AI 提供精准的上下文支持。

---

## MCP 服务集成

支持 MCP（Model Context Protocol）服务扩展。小伙伴 **llong** 开发了一个图片搜索的 MCP 服务，本地部署，可以快速响应用户的图片搜索需求。

---

## 工具调用（Tool Call）

AI 可以自主调用以下 **7** 个工具完成复杂任务：

| 工具名称 | 功能描述 |
|---------|---------|
| **FileOperationTool** | 包含读和写文件操作，主要用于生成 Markdown 文件 |
| **PDFGenerationTool** | 生成 PDF 文件 |
| **TerminalOperationTool** | 终端操作工具，让 AI 可以操作服务器终端，适配 Linux 和 Windows |
| **TerminateTool** | 让 AI 终止工具调用的工具 |
| **WebSearchTool** | 使用 Tavily 搜索，内容更适配 AI，效果更好，速度更快 |
| **WebScrapingTool** | 抓取网页内容，当搜索到符合的内容时进行深度解读，获取更完整的内容 |
| **ResourceDownloadTool** | 源码下载工具，可下载代码、图片等，只要包含可访问的 URL 即可下载 |

---

## 向量检索

用户提问时，向量数据库自动进行相似度检索，返回 **topK=3** 的最相关文档片段，确保回答准确有据。

---

## 双模式对话与记忆

- **寻觅聊天模式**：持久化存储到 PostgreSQL，支持滑动窗口记忆（最近 **20** 条），通过 **conversationID** 区分对话
- **Super Agent 模式**：独立 Agent 记忆空间，不持久化存储，保护对话隐私
`

onMounted(() => {
  content.value = marked(mdContent)
})
</script>

<template>
  <div class="capability-page">
    <div class="intro-header">
      <el-button text @click="router.push('/chat')">
        <el-icon><ArrowLeft /></el-icon>
        返回聊天
      </el-button>
      <span class="header-title">能力展示</span>
      <span class="header-spacer"></span>
    </div>

    <div class="intro-content">
      <div class="markdown-body" v-html="content"></div>
    </div>
  </div>
</template>

<style scoped>
.capability-page {
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
