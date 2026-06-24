export interface ApiEnvelope<T> {
  code: number
  message: string
  data: T
}

export interface PageResponse<T> {
  total: number
  list: T[]
}

export interface AiProviderConfig {
  id: number
  name: string
  provider: string
  baseUrl: string
  apiKeyMasked: string
  model: string
  temperature: number
  maxTokens: number
  topP: number
  timeoutMs: number
  responseFormat: string
  enabled: boolean
  priority: number
  weight: number
}

export interface AdminLoginResponse {
  token: string
  role: string
}

export interface AiProviderPayload {
  id?: number | null
  name: string
  provider: string
  baseUrl: string
  apiKey?: string
  model: string
  temperature: number
  maxTokens: number
  topP: number
  timeoutMs: number
  responseFormat: string
  enabled: boolean
  priority: number
  weight: number
}

export interface AiProviderTestResponse {
  success: boolean
  provider: string
  model: string
  message: string
}

export interface PromptTemplate {
  id: number
  sceneCode: string
  version: string
  systemPrompt: string
  userPromptTemplate: string
  schemaJson: string | null
  enabled: boolean
  remark: string | null
}

export interface PromptTemplatePayload {
  id?: number | null
  sceneCode: string
  version: string
  systemPrompt: string
  userPromptTemplate: string
  schemaJson?: string | null
  enabled: boolean
  remark?: string | null
}

export interface SensitiveWord {
  id: number
  word: string
  type: string
  createdAt: string
}

export interface SensitiveWordPayload {
  id?: number | null
  word: string
  type: string
}

export interface AdminDreamRecord {
  dreamRecordId: number
  dreamResultId: number | null
  userId: number
  dreamText: string
  tags: string[]
  summary: string
  resultJson: string | null
  provider: string | null
  model: string | null
  promptVersion: string | null
  tokenIn: number | null
  tokenOut: number | null
  status: string | null
  createdAt: string
  resultCreatedAt: string | null
}

type JsonBody = object

interface AdminRequestInit extends Omit<RequestInit, 'body'> {
  body?: BodyInit | JsonBody | null
}

const TOKEN_KEY = 'dream-admin-token'
const API_BASE = (import.meta.env.VITE_API_BASE_URL || '/api').replace(/\/$/, '')

export class ApiError extends Error {
  readonly status: number
  readonly code?: number

  constructor(message: string, status: number, code?: number) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.code = code
  }
}

export function apiBaseUrl() {
  return API_BASE
}

export function getToken() {
  return window.localStorage.getItem(TOKEN_KEY) || ''
}

export function setToken(token: string) {
  window.localStorage.setItem(TOKEN_KEY, token)
}

export function clearToken() {
  window.localStorage.removeItem(TOKEN_KEY)
}

export async function adminLogin(username: string, password: string) {
  return request<AdminLoginResponse>('/auth/adminLogin', {
    method: 'POST',
    body: { username, password },
  })
}

export async function request<T>(path: string, options: AdminRequestInit = {}): Promise<T> {
  const headers = new Headers(options.headers)
  const token = getToken()
  let body = options.body

  if (body && typeof body === 'object' && !(body instanceof FormData) && !(body instanceof URLSearchParams)) {
    body = JSON.stringify(body)
    headers.set('Content-Type', 'application/json')
  }

  if (token) {
    headers.set('Authorization', `Bearer ${token}`)
  }

  const response = await fetch(`${API_BASE}${path}`, {
    ...options,
    body: body as BodyInit | null | undefined,
    headers,
  })

  const payload = await parseEnvelope<T>(response)
  if (!response.ok) {
    throw new ApiError(payload?.message || `HTTP ${response.status}`, response.status, payload?.code)
  }
  if (!payload) {
    throw new ApiError('响应为空', response.status)
  }
  if (payload.code !== 0) {
    throw new ApiError(payload.message || '请求失败', response.status, payload.code)
  }
  return payload.data
}

async function parseEnvelope<T>(response: Response): Promise<ApiEnvelope<T> | null> {
  const text = await response.text()
  if (!text) {
    return null
  }
  try {
    return JSON.parse(text) as ApiEnvelope<T>
  } catch {
    return null
  }
}
