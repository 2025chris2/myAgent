import{At as e,Lt as t,Nt as n,Yt as r,an as i,i as a,mn as o,mt as s,o as c,pt as l,r as u,st as d,ut as f}from"./vue-router-DjNbPdGX.js";import{t as p}from"./marked.esm-BfWo0J-Q.js";var m={class:`capability-page`},h={class:`intro-header`},g={class:`intro-content`},_=[`innerHTML`],v=`
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
`,y=a({__name:`Capability`,setup(a){let y=u(),b=i(``);return e(()=>{b.value=p(v)}),(e,i)=>{let a=t(`el-icon`),u=t(`el-button`);return n(),f(`div`,m,[d(`div`,h,[s(u,{text:``,onClick:i[0]||=e=>o(y).push(`/chat`)},{default:r(()=>[s(a,null,{default:r(()=>[s(o(c))]),_:1}),i[1]||=l(` 返回聊天 `,-1)]),_:1}),i[2]||=d(`span`,{class:`header-title`},`能力展示`,-1),i[3]||=d(`span`,{class:`header-spacer`},null,-1)]),d(`div`,g,[d(`div`,{class:`markdown-body`,innerHTML:b.value},null,8,_)])])}}},[[`__scopeId`,`data-v-85b39b46`]]);export{y as default};