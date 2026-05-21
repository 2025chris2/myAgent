### 大纲
一个完整的AI Agent项目，包括前端后端的交互，项目的重心是后端AI部分，以及前端对后端AI的回应进行怎么样的渲染

# llongAgent - AI 智能对话平台
> 基于 Spring Boot + Spring AI + Vue3 构建的前后端分离式 AI 智能对话平台，支持**RAG 检索增强、自研 ReAct 智能体、工具调用、双模式对话、流式 SSE 输出**，可实现旅行定制、复杂任务自主执行、文件生成、网页检索等能力。

## 📋 项目简介
llongAgent 是一款功能强大的 AI 智能对话平台，提供两种核心聊天模式：**寻觅聊天（PlanApp）** 与 **Super Agent 通用智能体**。
- 寻觅聊天：专注旅行定制场景，内置旅行知识库，结合 RAG 精准规划出行方案
- Super Agent：自研 ReAct 代理框架，支持多工具调用循环，自主完成复杂任务
- 整体采用前后端分离架构，后端 Java 21 技术栈，前端 Vue3 生态，向量数据库 PostgreSQL+pgvector 实现 RAG，JWT 完成身份认证。

## ✨ 核心能力
### 1. RAG 检索增强生成
- 内置海量旅行领域知识库，覆盖旅行规划、目的地推荐、出行全流程、300+ 热点旅行问题
- 基于 **pgvector** 向量相似度检索，查询自动重写，返回 TopK=3 相关文档片段，保证回答精准有据

### 2. MCP 服务集成
兼容 **MCP（Model Context Protocol）** 协议，可对接第三方 MCP 服务；已集成图片搜索 MCP 服务，本地部署快速响应图像检索需求，可灵活扩展更多能力。

### 3. 7 大 AI 可调用工具
Agent 模式下 AI 自主决策调用工具，完成复杂任务，工具列表如下：

| 工具名称 | 功能描述 |
| :--- | :--- |
| FileOperationTool | 读写文件，主要用于生成 Markdown 文档 |
| PDFGenerationTool | 一键生成 PDF 文件 |
| TerminalOperationTool | 跨系统终端操作，适配 Linux / Windows |
| TerminateTool | 主动终止工具调用循环 |
| WebSearchTool | 基于 Tavily 智能搜索，AI 友好、高速精准 |
| WebScrapingTool | 网页深度抓取，解析搜索结果完整内容 |
| ResourceDownloadTool | 资源下载，支持代码、图片、文件等 URL 资源 |

### 4. 向量检索
用户提问自动向量化，与知识库向量比对，检索最相关上下文，避免 AI 幻觉，提升回答真实性。

### 5. 双模式对话与差异化记忆
- **寻觅聊天（PlanApp）**：对话持久化至 PostgreSQL，按会话 ID 隔离，滑动窗口记忆（最近20条消息）
- **Super Agent 模式**：独立内存级 Agent 记忆，**不持久化存储**，保护对话隐私

## 🧱 技术架构
### 后端技术栈
- 核心：**Java 21 + Spring Boot 3.5.14 + Spring AI 1.1.5**
- 数据层：PostgreSQL + pgvector（向量库）、Redis、JPA、Flyway
- 安全：Spring Security、JWT 无状态认证、BCrypt 密码加密
- 通信：SSE 流式推送，实时输出思考、工具调用、回答内容

#### 后端分层架构
```
├── Controller层        # Auth/Conversation/AiController，REST API + SSE 流式输出
├── App层               # PlanApp.java 寻觅聊天，DeepSeek + RAG + 工具调用
├── Agent层             # 自研ReAct框架，BaseAgent→ReActAgent→ToolCallAgent→llongManus
├── Tools层             # 7个AI可调用工具实现
├── RAG层               # 向量检索、文档加载、查询重写
├── Security层          # JWT认证、权限配置、密码加密
```

#### 数据库设计
共3张核心表，ID 采用6位随机ID保证安全性：
- `users`：用户信息表（6位随机ID）
- `conversations`：会话表（6位随机ID）
- `messages`：消息表（自增ID，JSON存储对话消息）

### 前端技术栈
- 框架：Vue3 + Vite + Element Plus + Pinia
- 路由：`/login`（登录注册）、`/chat`（主聊天页面，需JWT鉴权）

#### 组件结构
```
App.vue
└── Chat.vue
    ├── Sidebar.vue      # 会话列表，新建/删除会话
    └── ChatPanel.vue    # 模式切换、消息列表、输入框
        ├── MessageList.vue
        │   └── MessageBubble.vue # 思考面板、消息内容、下载链接
        └── InputArea.vue # 消息输入、同步/异步切换、发送/停止
```

#### 状态管理（Pinia）
- `auth.js`：Token、用户名持久化存储至 localStorage
- `chat.js`：会话管理、消息管理、SSE 流式解析、双模式调度

## 🚀 前端亮点功能
1. **自研 SSE 解析器**：`streamRequest`，完美兼容 Spring SseEmitter 流式格式
2. **Agent 思考面板**：实时展示推理链路：思考 → 工具调用 → 工具结果 → 最终答案
3. **自动文件下载**：AI 生成 Markdown/PDF 后，前端自动触发浏览器下载
4. **链接自动渲染**：文件下载接口自动转为可点击链接，一键获取生成资源

## 📦 快速部署
### 环境要求
- JDK 21+
- Node.js 18+
- PostgreSQL 15+（开启 pgvector 插件）
- Redis
- Maven / pnpm

### 后端部署
1. 克隆项目，配置 `application.yml` 数据库、Redis、DeepSeek API、Tavily API、JWT 密钥
2. 执行 Flyway 自动初始化数据库
3. Maven 打包启动 Spring Boot 项目

### 前端部署
1. 进入前端目录，`pnpm install` 安装依赖
2. 配置后端接口地址
3. `pnpm dev` 启动开发环境，`pnpm build` 打包部署

## 📌 模式使用说明
### 寻觅聊天（PlanApp）
- 定位：旅行定制专家
- 特性：预设旅行领域 System Prompt、RAG 知识库加持、持久化对话、滑动窗口记忆、同步/异步 SSE

### Super Agent
- 定位：通用复杂任务智能体
- 特性：自研 ReAct 循环（最多20步）、7大工具自主调用、SSE 结构化推送事件、内存级记忆、隐私安全

## 📄 许可证
MIT License
