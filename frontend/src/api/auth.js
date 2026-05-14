import { post } from './request.js'

export function register(username, password) {
  return post('/api/auth/register', { username, password })
}

export function login(username, password) {
  return post('/api/auth/login', { username, password })
}
