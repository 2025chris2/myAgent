import { get, post, del } from './request.js'

export function createConversation() {
  return post('/api/conversations')
}

export function getConversations() {
  return get('/api/conversations')
}

export function getMessages(conversationId) {
  return get(`/api/conversations/${conversationId}/messages`)
}

export function deleteConversation(id) {
  return del(`/api/conversations/${id}`)
}
