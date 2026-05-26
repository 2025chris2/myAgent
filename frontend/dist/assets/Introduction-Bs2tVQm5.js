import{At as e,Lt as t,Nt as n,Yt as r,an as i,i as a,mn as o,mt as s,o as c,pt as l,r as u,st as d,ut as f}from"./vue-router-DjNbPdGX.js";import{t as p}from"./marked.esm-BfWo0J-Q.js";var m={class:`intro-page`},h={class:`intro-header`},g={class:`intro-content`},_=[`innerHTML`],v=`
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
`,y=a({__name:`Introduction`,setup(a){let y=u(),b=i(``);return e(()=>{b.value=p(v)}),(e,i)=>{let a=t(`el-icon`),u=t(`el-button`);return n(),f(`div`,m,[d(`div`,h,[s(u,{text:``,onClick:i[0]||=e=>o(y).push(`/chat`)},{default:r(()=>[s(a,null,{default:r(()=>[s(o(c))]),_:1}),i[1]||=l(` 返回聊天 `,-1)]),_:1}),i[2]||=d(`span`,{class:`header-title`},`项目总览`,-1),i[3]||=d(`span`,{class:`header-spacer`},null,-1)]),d(`div`,g,[d(`div`,{class:`markdown-body`,innerHTML:b.value},null,8,_)])])}}},[[`__scopeId`,`data-v-ac3ae950`]]);export{y as default};