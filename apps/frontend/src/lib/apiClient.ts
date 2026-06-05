import axios, { AxiosError, type AxiosRequestConfig } from 'axios'
import { ApiError } from './apiError'
import { notifySessionExpired } from './authSession'
import {
  clearAuthTokens,
  getAuthorizationHeader,
  getRefreshToken,
  saveAuthTokens,
} from './authToken'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080'
const REFRESH_TOKEN_PATH = '/v1/auth/token/refresh'

interface AuthTokensResponse {
  accessToken: string
  refreshToken: string
  tokenType: string
  expiresInSeconds: number
}

interface RetryableRequestConfig extends AxiosRequestConfig {
  _retry?: boolean
}

interface ErrorResponseBody {
  message?: string
  title?: string
  detail?: string
  path?: string
  instance?: string
  traceId?: string
  trace_id?: string
  requestId?: string
  [key: string]: unknown
}

const PUBLIC_AUTH_ENDPOINTS = [
  '/v1/auth/login',
  '/v1/auth/signup',
  '/v1/auth/verify',
  '/v1/auth/oauth',
  '/v1/auth/password',
  REFRESH_TOKEN_PATH,
  '/v1/auth/logout',
]

let refreshPromise: Promise<AuthTokensResponse> | null = null

export const apiClient = axios.create({
  baseURL: API_BASE_URL,
  timeout: 10_000,
  headers: {
    'Content-Type': 'application/json',
  },
})

apiClient.interceptors.request.use((config) => {
  const authorizationHeader = getAuthorizationHeader()

  if (authorizationHeader) {
    config.headers.Authorization = authorizationHeader
  }

  return config
})

apiClient.interceptors.response.use(
  (response) => response,
  async (error: AxiosError<ErrorResponseBody>) => {
    const requestConfig = error.config as RetryableRequestConfig | undefined

    if (shouldRefresh(error, requestConfig)) {
      try {
        await refreshAccessToken()

        if (requestConfig) {
          requestConfig._retry = true
          return apiClient.request(requestConfig)
        }
      } catch {
        clearAuthTokens()
        notifySessionExpired()
      }
    }

    throw normalizeApiError(error)
  },
)

function shouldRefresh(
  error: AxiosError<ErrorResponseBody>,
  requestConfig: RetryableRequestConfig | undefined,
) {
  return (
    error.response?.status === 401 &&
    Boolean(requestConfig) &&
    requestConfig?._retry !== true &&
    !isPublicAuthEndpoint(requestConfig?.url)
  )
}

async function refreshAccessToken() {
  if (!refreshPromise) {
    const refreshToken = getRefreshToken()

    if (!refreshToken) {
      return Promise.reject(new Error('Missing refresh token.'))
    }

    refreshPromise = axios
      .post<AuthTokensResponse>(
        `${API_BASE_URL}${REFRESH_TOKEN_PATH}`,
        { refreshToken },
        {
          timeout: 10_000,
          headers: {
            'Content-Type': 'application/json',
          },
        },
      )
      .then((response) => {
        saveAuthTokens(response.data)
        return response.data
      })
      .finally(() => {
        refreshPromise = null
      })
  }

  return refreshPromise
}

function isPublicAuthEndpoint(url: string | undefined) {
  if (!url) {
    return false
  }

  const path = normalizePath(url)
  return PUBLIC_AUTH_ENDPOINTS.some(
    (endpoint) => path === endpoint || path.startsWith(`${endpoint}/`),
  )
}

function normalizePath(url: string) {
  try {
    const parsedUrl = new URL(url, API_BASE_URL)
    return parsedUrl.pathname
  } catch {
    return url.split('?')[0] || url
  }
}

function normalizeApiError(error: AxiosError<ErrorResponseBody>) {
  const requestConfig = error.config as RetryableRequestConfig | undefined
  const method = requestConfig?.method?.toUpperCase() ?? 'GET'
  const path = requestConfig?.url ? normalizePath(requestConfig.url) : null

  if (error.response) {
    const details = error.response.data
    const message =
      details?.message ||
      details?.detail ||
      details?.title ||
      `${method} ${path ?? ''} failed (${error.response.status})`
    const traceId =
      details?.traceId ||
      details?.trace_id ||
      details?.requestId ||
      getHeaderValue(error.response.headers?.['x-trace-id']) ||
      getHeaderValue(error.response.headers?.['x-request-id'])

    return new ApiError({
      status: error.response.status,
      message,
      path: details?.path || details?.instance || path,
      traceId,
      retryable: isRetryableStatus(error.response.status),
      details,
    })
  }

  if (error.code === 'ECONNABORTED') {
    return new ApiError({
      status: null,
      message: 'The request timed out. Please try again.',
      path,
      traceId: null,
      retryable: true,
      details: error.toJSON(),
    })
  }

  if (error.request) {
    return new ApiError({
      status: null,
      message: 'Unable to reach the server. Check your connection and try again.',
      path,
      traceId: null,
      retryable: true,
      details: error.toJSON(),
    })
  }

  return new ApiError({
    status: null,
    message: error.message || 'Unexpected API error.',
    path,
    traceId: null,
    retryable: false,
    details: error.toJSON(),
  })
}

function isRetryableStatus(status: number) {
  return status === 408 || status === 409 || status === 425 || status === 429 || status >= 500
}

function getHeaderValue(value: unknown) {
  if (Array.isArray(value)) {
    return value[0] ?? null
  }

  return typeof value === 'string' ? value : null
}
