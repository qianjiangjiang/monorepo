import { clearToken, getToken } from '../utils/storage'

export interface ApiEnvelope<T> {
  code: number
  message: string
  data: T
}

export class ApiError extends Error {
  code: number
  statusCode: number

  constructor(message: string, code = -1, statusCode = 0) {
    super(message)
    this.name = 'ApiError'
    this.code = code
    this.statusCode = statusCode
  }
}

interface RequestOptions {
  url: string
  method?: 'GET' | 'POST' | 'PUT' | 'DELETE'
  data?: unknown
  auth?: boolean
  loading?: boolean
  loadingText?: string
  silent?: boolean
}

const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL || '/api').replace(/\/$/, '')
let interceptorsReady = false

function normalizeUrl(url: string) {
  if (/^https?:\/\//.test(url)) {
    return url
  }

  if (url.startsWith('/api')) {
    return `${API_BASE_URL}${url.slice(4)}`
  }

  return `${API_BASE_URL}${url.startsWith('/') ? url : `/${url}`}`
}

function getErrorMessage(code: number, fallback: string) {
  if (code === 401) {
    return '登录已过期，请重新登录'
  }
  if (code === 429) {
    return '今日解梦次数已用完，请稍后再试'
  }
  if (code >= 500) {
    return '服务暂时不可用，请稍后重试'
  }
  return fallback || '请求失败，请稍后重试'
}

export function setupRequestInterceptors() {
  if (interceptorsReady) {
    return
  }

  uni.addInterceptor('request', {
    invoke(args) {
      const token = getToken()
      const shouldAuth = !String(args.url || '').includes('/auth/wxLogin')
      args.header = {
        ...(args.header || {}),
        ...(token && shouldAuth ? { Authorization: `Bearer ${token}` } : {}),
      }
    },
  })

  interceptorsReady = true
}

export function request<T>(options: RequestOptions): Promise<T> {
  const {
    url,
    method = 'GET',
    data,
    auth = true,
    loading = true,
    loadingText = '加载中',
    silent = false,
  } = options
  const token = getToken()

  if (auth && !token) {
    return Promise.reject(new ApiError('请先登录', 401, 401))
  }

  if (loading) {
    uni.showLoading({ title: loadingText, mask: true })
  }

  return new Promise<T>((resolve, reject) => {
    uni.request({
      url: normalizeUrl(url),
      method,
      data: data as UniApp.RequestOptions['data'],
      timeout: 15000,
      header: {
        'Content-Type': 'application/json',
        ...(auth && token ? { Authorization: `Bearer ${token}` } : {}),
      },
      success(response) {
        const statusCode = response.statusCode
        const body = response.data as Partial<ApiEnvelope<T>>
        const code = typeof body?.code === 'number' ? body.code : statusCode
        const message = getErrorMessage(code, body?.message || '')

        if (statusCode >= 200 && statusCode < 300 && code === 0) {
          resolve(body.data as T)
          return
        }

        if (code === 401 || statusCode === 401) {
          clearToken()
        }

        const error = new ApiError(message, code, statusCode)
        if (!silent) {
          uni.showToast({ title: error.message, icon: 'none' })
        }
        reject(error)
      },
      fail() {
        const error = new ApiError('网络连接异常，请检查后重试')
        if (!silent) {
          uni.showToast({ title: error.message, icon: 'none' })
        }
        reject(error)
      },
      complete() {
        if (loading) {
          uni.hideLoading()
        }
      },
    })
  })
}
