import { defineStore } from 'pinia'
import { ref, reactive } from 'vue'
import {
  createConversation,
  getConversations,
  getMessages as getConvMessages,
  deleteConversation as deleteConvApi,
} from '../api/conversation.js'
import { get, streamRequest } from '../api/request.js'
import { extractDownloadUrls, downloadFile } from '../utils/download.js'

export const useChatStore = defineStore('chat', () => {
  const conversations = ref([])
  const currentConversationId = ref('')
  const messages = reactive({})
  const chatMode = ref('planapp') // 'planapp' | 'agent'
  const syncMode = ref('async') // 'sync' | 'async' (only for planapp)
  const streaming = ref(false)
  let currentController = null

  // ===== Conversations =====
  async function fetchConversations() {
    conversations.value = await getConversations()
  }

  async function createNewConversation() {
    const data = await createConversation()
    currentConversationId.value = data.conversationId
    messages[data.conversationId] = []
    await fetchConversations()
    return data.conversationId
  }

  async function removeConversation(id) {
    await deleteConvApi(id)
    conversations.value = conversations.value.filter((c) => c.id !== id)
    delete messages[id]
    if (currentConversationId.value === id) {
      currentConversationId.value = conversations.value[0]?.id || ''
    }
  }

  let loadVersion = 0

  async function setCurrentConversation(id) {
    currentConversationId.value = id
    // Already loaded — skip fetching
    if (messages[id] && messages[id].length > 0) return
    if (!messages[id]) {
      messages[id] = []
    }

    const version = ++loadVersion
    try {
      const msgs = await getConvMessages(id)
      // Only apply if this is still the latest request
      if (version === loadVersion && currentConversationId.value === id) {
        messages[id] = msgs || []
      }
    } catch (err) {
      if (version === loadVersion && currentConversationId.value === id) {
        messages[id] = []
      }
    }
  }

  // ===== Messages =====
  function getMessages(convId) {
    return messages[convId] || []
  }

  function addMessage(convId, msg) {
    if (!messages[convId]) {
      messages[convId] = []
    }
    messages[convId].push(msg)
  }

  // Returns the reactive proxy of the last message in a conversation.
  // MUST be used instead of a local plain-object reference so that
  // mutations to .content are tracked by Vue's reactivity system.
  function getLastAssistantMessage(convId) {
    const msgs = messages[convId]
    if (!msgs || msgs.length === 0) return null
    return msgs[msgs.length - 1]
  }

  // ===== Send Message =====
  async function sendMessage(content) {
    let convId = currentConversationId.value
    if (!convId) {
      convId = await createNewConversation()
    }

    // Add user message
    addMessage(convId, {
      id: Date.now().toString(),
      role: 'user',
      content,
      time: new Date().toLocaleTimeString(),
    })

    // Add assistant message placeholder
    addMessage(convId, {
      id: (Date.now() + 1).toString(),
      role: 'assistant',
      content: '',
      time: new Date().toLocaleTimeString(),
    })

    streaming.value = true

    if (chatMode.value === 'planapp' && syncMode.value === 'sync') {
      // PlanApp sync — plain text response
      try {
        const text = await get('/plan_app/ai/chat/sync', {
          userMessage: content,
          conversationId: convId,
        })
        const msg = getLastAssistantMessage(convId)
        if (msg) msg.content = text
      } catch (err) {
        const msg = getLastAssistantMessage(convId)
        if (msg) msg.content = `[错误] ${err.message}`
      } finally {
        streaming.value = false
      }
    } else if (chatMode.value === 'planapp' && syncMode.value === 'async') {
      // PlanApp async — SSE streaming
      currentController = streamRequest(
        '/plan_app/ai/chat/async',
        { userMessage: content, conversationId: convId },
        {
          onChunk(text) {
            const msg = getLastAssistantMessage(convId)
            if (msg) msg.content += text
          },
          onDone() {
            streaming.value = false
            currentController = null
          },
          onError(err) {
            const msg = getLastAssistantMessage(convId)
            if (msg) msg.content += `\n[错误] ${err}`
            streaming.value = false
            currentController = null
          },
        },
      )
    } else if (chatMode.value === 'agent') {
      // Super Agent — SSE streaming with typed events
      const msg = getLastAssistantMessage(convId)
      if (msg) msg.thinkLines = []

      const pendingDownloads = new Set()

      currentController = streamRequest(
        '/plan_app/ai/manus/chat',
        { userMessage: content, conversationId: convId },
        {
          onChunk(_text) {
            // Legacy fallback — raw text treated as final answer
          },
          onTypedEvent(type, data) {
            const m = getLastAssistantMessage(convId)
            if (!m) return
            switch (type) {
              case 'thinking':
              case 'tool_call':
              case 'tool_result':
                if (!m.thinkLines) m.thinkLines = []
                m.thinkLines.push({ type, data })
                extractDownloadUrls(data).forEach(u => pendingDownloads.add(u))
                break
              case 'final_answer':
                m.content += data
                extractDownloadUrls(data).forEach(u => pendingDownloads.add(u))
                break
              case 'error':
                m.content += `\n[错误] ${data}`
                streaming.value = false
                currentController = null
                break
              case 'done':
                streaming.value = false
                currentController = null
                if (pendingDownloads.size > 0) {
                  ;(async () => {
                    for (const url of pendingDownloads) {
                      try { await downloadFile(url) }
                      catch (e) { console.error('自动下载失败:', url, e) }
                    }
                  })()
                }
                break
            }
          },
          onDone() {
            streaming.value = false
            currentController = null
          },
          onError(err) {
            const m = getLastAssistantMessage(convId)
            if (m) {
              if (!m.thinkLines) m.thinkLines = []
              m.thinkLines.push({ type: 'error', data: err })
            }
            streaming.value = false
            currentController = null
          },
        },
      )
    }
  }

  function cancelStream() {
    if (currentController) {
      currentController.abort()
      currentController = null
      streaming.value = false
    }
  }

  return {
    conversations,
    currentConversationId,
    messages,
    chatMode,
    syncMode,
    streaming,
    fetchConversations,
    createNewConversation,
    removeConversation,
    setCurrentConversation,
    getMessages,
    sendMessage,
    cancelStream,
  }
})
