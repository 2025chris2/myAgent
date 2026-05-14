# llongAgent 后端 API 接口文档

> 基础地址：`http://localhost:8080`

---

## 一、认证机制

所有业务接口（除了注册/登录）都需要在请求头携带 JWT：

```
Authorization: Bearer <token>
```

token 通过注册或登录接口获取。

---

## 二、接口列表

### 1. 用户注册

```
POST /api/auth/register
```

**请求体 (JSON)：**

```json
{
  "username": "tzl",
  "password": "123456"
}
```

约束：`username` 3~50字符，`password` 最少6位。

**成功响应 (200)：**

```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "userId": null
}
```

**错误响应：**

| HTTP 状态 | 说明 |
|-----------|------|
| 400 | 参数校验失败（用户名/密码不符合要求） |
| 500 | 用户名已存在：`用户名已存在` |

---

### 2. 用户登录

```
POST /api/auth/login
```

**请求体 (JSON)：**

```json
{
  "username": "tzl",
  "password": "123456"
}
```

**成功响应 (200)：**

```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "userId": null
}
```

**错误响应：**

| HTTP 状态 | 说明 |
|-----------|------|
| 500 | 用户名或密码错误 |

---

### 3. 新建会话

```
POST /api/conversations
Authorization: Bearer <token>
```

**请求体：** 无

**成功响应 (200)：**

```json
{
  "conversationId": "aB3xK9"
}
```

`conversationId` 是 6 位随机字符串，后续所有聊天接口都需要它。

---

### 4. 获取会话列表

```
GET /api/conversations
Authorization: Bearer <token>
```

**成功响应 (200)：**

```json
[
  {
    "id": "aB3xK9",
    "title": "",
    "createdAt": "2026-05-09T12:00:00"
  }
]
```

---

### 5. 删除会话

```
DELETE /api/conversations/{conversationId}
Authorization: Bearer <token>
```

**成功响应：** 204 No Content

---

### 6. Agent 聊天（SSE 流式） ★ 核心接口

```
GET /plan_app/ai/manus/chat?userMessage=帮我生成泰山旅行PDF&conversationId=aB3xK9
Authorization: Bearer <token>
```

**参数：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `userMessage` | String | 是 | 用户输入的消息 |
| `conversationId` | String | **否** | 不传则后端自动创建新会话 |

**首次对话（不传 conversationId）：** 后端自动创建会话，建议前端从 SSE 事件中解析生成的 conversationId。当前版本 SSE 不返回 conversationId —— **所以首次对话前，前端应先调 `POST /api/conversations` 获取 ID**。

**响应：SSE 流 (text/event-stream)**

每个 SSE 事件格式：

```
data: Step 1:思考完成 - 无需行动！
data: Step 2:工具 webSearch 返回的结果：...
data: Step 3:思考完成 - 无需行动！
data: Reached maxStep!
```

前端读取示例：

```javascript
// 使用 EventSource（仅 GET 请求，不支持自定义 Header）
// 改用 fetch + ReadableStream 手动处理

async function chat(userMessage, conversationId, token) {
  const url = new URL('/plan_app/ai/manus/chat', 'http://localhost:8080');
  url.searchParams.set('userMessage', userMessage);
  if (conversationId) {
    url.searchParams.set('conversationId', conversationId);
  }

  const response = await fetch(url, {
    headers: { 'Authorization': `Bearer ${token}` }
  });

  const reader = response.body.getReader();
  const decoder = new TextDecoder();

  while (true) {
    const { done, value } = await reader.read();
    if (done) break;
    const text = decoder.decode(value, { stream: true });
    // text 为 SSE 原始数据，需自行解析 "data: ..." 行
    console.log(text);
  }
}
```

---

### 7. PlanApp 同步聊天

```
GET /plan_app/ai/chat/sync?userMessage=你好&conversationId=aB3xK9
Authorization: Bearer <token>
```

**参数：** 同上（`userMessage` 必填，`conversationId` 可选）

**响应 (200)：** 纯文本字符串

---

### 8. PlanApp 流式聊天（SSE）

```
GET /plan_app/ai/chat/async?userMessage=你好&conversationId=aB3xK9
Authorization: Bearer <token>
```

**参数：** 同上

**响应：** SSE 流，每个 chunk 是一段文本片段

---

## 三、conversationId 交互流程（重点）

```
┌─────────────────────────────────────────────────────┐
│                   前端交互流程                         │
├─────────────────────────────────────────────────────┤
│                                                       │
│  1. 注册/登录                                          │
│     POST /api/auth/register  → 拿到 token              │
│     POST /api/auth/login     → 拿到 token              │
│                                                       │
│  2. 新建对话（用户点击"新对话"按钮时）                    │
│     POST /api/conversations   → 拿到 conversationId     │
│     （存到前端状态中）                                   │
│                                                       │
│  3. 发送消息                                           │
│     GET /plan_app/ai/manus/chat                       │
│       ?userMessage=用户输入                             │
│       &conversationId=第2步拿到的ID                     │
│     → SSE 流式接收回复                                 │
│                                                       │
│  4. 继续对话（同一会话发第二条消息）                      │
│     直接用第2步的 conversationId + 新的 userMessage      │
│     → SSE 流式接收回复（上下文自动关联）                  │
│                                                       │
│  5. 获取历史会话列表                                    │
│     GET /api/conversations → 展示在左侧列表              │
│                                                       │
│  6. 点击历史会话                                        │
│     从列表拿到 conversationId                          │
│     调聊天接口（传 conversationId）                      │
│     → 后端自动加载该会话的历史消息作为上下文              │
│                                                       │
└─────────────────────────────────────────────────────┘
```

**关键规则：**

- `conversationId` 是 6 位字符串（如 `aB3xK9`），由后端生成，前端**不要自己生成**
- 同一 conversationId 下的消息共享上下文（LLM 记忆）
- 不同 conversationId 之间的上下文完全隔离
- 不传 conversationId 时后端自动创建新会话（但当前不会在 SSE 中返回 ID，建议前端主动调新建接口）

---

## 四、请求 Header 汇总

| Header | 值 | 哪些接口需要 |
|--------|-----|-------------|
| `Authorization` | `Bearer <token>` | 除注册/登录外的所有接口 |
| `Content-Type` | `application/json` | POST 接口（注册、登录、新建会话） |

---

## 五、前端最小可跑示例

```javascript
const BASE = 'http://localhost:8080';

// 1. 注册
async function register(username, password) {
  const res = await fetch(`${BASE}/api/auth/register`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username, password })
  });
  const data = await res.json();
  return data.token;  // 存到 localStorage
}

// 2. 新建会话
async function newConversation(token) {
  const res = await fetch(`${BASE}/api/conversations`, {
    method: 'POST',
    headers: { 'Authorization': `Bearer ${token}` }
  });
  const data = await res.json();
  return data.conversationId;  // 6位字符串
}

// 3. 发送消息（SSE 流式）
async function sendMessage(userMessage, conversationId, token, onChunk) {
  const url = `${BASE}/plan_app/ai/manus/chat?userMessage=${encodeURIComponent(userMessage)}&conversationId=${conversationId}`;
  const res = await fetch(url, {
    headers: { 'Authorization': `Bearer ${token}` }
  });
  const reader = res.body.getReader();
  const decoder = new TextDecoder();
  while (true) {
    const { done, value } = await reader.read();
    if (done) break;
    onChunk(decoder.decode(value, { stream: true }));
  }
}

// 4. 获取历史会话列表
async function getConversations(token) {
  const res = await fetch(`${BASE}/api/conversations`, {
    headers: { 'Authorization': `Bearer ${token}` }
  });
  return res.json();  // [{id, title, createdAt}, ...]
}
```

---

## 六、注意事项

1. **跨域**：后端已全局开放 CORS（`CorsConfig.java`），前端无需额外配置
2. **token 有效期**：默认 24 小时（配置在 `application-local.yaml` 的 `jwt.expiration`）
3. **SSE 超时**：Agent 聊天超时 5 分钟，PlanApp 流式超时 3 分钟
4. **Knife4j 文档**：浏览器访问 `http://localhost:8080/doc.html` 可在线测试所有接口
