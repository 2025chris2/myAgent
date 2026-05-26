const BASE = ''

function getToken() {
  return localStorage.getItem('token')
}

function handleAuthFailure() {
  localStorage.removeItem('token')
  localStorage.removeItem('username')
  window.location.href = '/login'
}

export async function request(method, path, body, options = {}) {
  const headers = { ...(options.headers || {}) }

  if (body) {
    headers['Content-Type'] = 'application/json'
  }

  const token = getToken()
  if (token) {
    headers['Authorization'] = `Bearer ${token}`
  }

  const res = await fetch(`${BASE}${path}`, {
    method,
    headers,
    body: body ? JSON.stringify(body) : undefined,
  })

  if (res.status === 204) {
    return null
  }

  const data = res.headers.get('content-type')?.includes('application/json')
    ? await res.json()
    : await res.text()

  if (!res.ok) {
    if (res.status === 401 || res.status === 403) {
      handleAuthFailure()
      throw new Error('鉴权失败，请重新登录')
    }
    const message = typeof data === 'string' ? data : (data.message || data.error || '请求失败')
    throw new Error(message)
  }

  return data
}

export function get(path, params = {}) {
  const query = new URLSearchParams(params).toString()
  const url = query ? `${path}?${query}` : path
  return request('GET', url)
}

export function post(path, body) {
  return request('POST', path, body)
}

export function del(path) {
  return request('DELETE', path)
}

/**
 * SSE streaming request using fetch + ReadableStream.
 * Correctly handles Spring SseEmitter format (data:content\n\n) and
 * multi-line SSE events (consecutive data: lines joined by \n).
 *
 * @param {string} path - API path
 * @param {object} params - URL query params
 * @param {function} onChunk - called with each complete SSE event payload
 * @param {function} onDone - called when stream ends
 * @param {function} onError - called on error
 * @returns {AbortController} - call .abort() to cancel
 */
export function streamRequest(path, params = {}, { onChunk, onTypedEvent, onDone, onError } = {}) {
  const controller = new AbortController()
  const query = new URLSearchParams(params).toString()
  const url = query ? `${BASE}${path}?${query}` : `${BASE}${path}`

  function dispatchPayload(payload, onChunkFallback, onTyped) {
    if (!payload) return
    // Try JSON typed event: {"type":"...","data":"..."}
    if (onTyped) {
      try {
        const parsed = JSON.parse(payload)
        if (parsed.type && 'data' in parsed) {
          onTyped(parsed.type, parsed.data)
          return
        }
      } catch (_) {
        // Not JSON, fall through to raw chunk
      }
    }
    if (onChunkFallback) onChunkFallback(payload)
  }

  const headers = {}
  const token = getToken()
  if (token) {
    headers['Authorization'] = `Bearer ${token}`
  }

  fetch(url, {
    headers,
    signal: controller.signal,
  })
    .then(async (res) => {
      if (!res.ok) {
        if (res.status === 401 || res.status === 403) {
          handleAuthFailure()
          throw new Error('鉴权失败，请重新登录')
        }
        const text = await res.text()
        throw new Error(text || '请求失败')
      }
      const reader = res.body.getReader()
      const decoder = new TextDecoder()
      let buffer = ''
      let eventDataLines = []

      while (true) {
        const { done, value } = await reader.read()
        if (done) break

        buffer += decoder.decode(value, { stream: true })

        // Split on \n (SSE uses \n as line separator)
        const lines = buffer.split('\n')
        // Last element might be incomplete — keep it in buffer for next round
        buffer = lines.pop() || ''

        for (const line of lines) {
          // Empty line signals end of an SSE event
          if (line === '' || line === '\r') {
            if (eventDataLines.length > 0) {
              const payload = eventDataLines.join('\n')
              dispatchPayload(payload, onChunk, onTypedEvent)
              eventDataLines = []
            }
            continue
          }

          // SSE comment line — skip
          if (line.startsWith(':')) continue

          // data: line — accumulate
          if (line.startsWith('data: ')) {
            eventDataLines.push(line.slice(6))
          } else if (line.startsWith('data:')) {
            eventDataLines.push(line.slice(5))
          } else if (line.trim()) {
            // Non-SSE format fallback — treat as raw content
            if (onChunk) onChunk(line)
          }
        }
      }

      // Flush any remaining event
      if (eventDataLines.length > 0) {
        const payload = eventDataLines.join('\n')
        dispatchPayload(payload, onChunk, onTypedEvent)
      }

      // Flush remaining buffer (incomplete line after stream ends)
      if (buffer.trim()) {
        if (buffer.startsWith('data: ')) {
          const chunk = buffer.slice(6)
          dispatchPayload(chunk, onChunk, onTypedEvent)
        } else if (buffer.startsWith('data:')) {
          const chunk = buffer.slice(5)
          dispatchPayload(chunk, onChunk, onTypedEvent)
        } else if (onChunk) {
          onChunk(buffer)
        }
      }

      if (onDone) onDone()
    })
    .catch((err) => {
      if (err.name !== 'AbortError' && onError) {
        onError(err.message || '流式请求失败')
      }
    })

  return controller
}
