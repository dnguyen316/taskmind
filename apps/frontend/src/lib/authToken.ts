const ACCESS_TOKEN_KEY = 'taskmind.accessToken'
const REFRESH_TOKEN_KEY = 'taskmind.refreshToken'
const TOKEN_TYPE_KEY = 'taskmind.tokenType'
const TOKEN_EXPIRES_AT_KEY = 'taskmind.tokenExpiresAt'

export interface StoredAuthTokens {
  accessToken: string
  refreshToken: string
  tokenType: string
  expiresInSeconds: number
}

export interface StoredAuthSession {
  accessToken: string
  refreshToken: string
  tokenType: string
  expiresAt: number
}

function storageAvailable() {
  return typeof window !== 'undefined' && typeof window.localStorage !== 'undefined'
}

export function saveAuthTokens(tokens: StoredAuthTokens) {
  if (!storageAvailable()) {
    return
  }

  const tokenType = tokens.tokenType || 'Bearer'
  const expiresAt = Date.now() + tokens.expiresInSeconds * 1000

  window.localStorage.setItem(ACCESS_TOKEN_KEY, tokens.accessToken)
  window.localStorage.setItem(REFRESH_TOKEN_KEY, tokens.refreshToken)
  window.localStorage.setItem(TOKEN_TYPE_KEY, tokenType)
  window.localStorage.setItem(TOKEN_EXPIRES_AT_KEY, String(expiresAt))
}

export function clearAuthTokens() {
  if (!storageAvailable()) {
    return
  }

  window.localStorage.removeItem(ACCESS_TOKEN_KEY)
  window.localStorage.removeItem(REFRESH_TOKEN_KEY)
  window.localStorage.removeItem(TOKEN_TYPE_KEY)
  window.localStorage.removeItem(TOKEN_EXPIRES_AT_KEY)
}

export function getAccessToken() {
  if (!storageAvailable()) {
    return null
  }

  return window.localStorage.getItem(ACCESS_TOKEN_KEY)
}

export function getAuthorizationHeader() {
  const accessToken = getAccessToken()

  if (!accessToken || !storageAvailable()) {
    return null
  }

  const tokenType = window.localStorage.getItem(TOKEN_TYPE_KEY) || 'Bearer'
  return `${tokenType} ${accessToken}`
}


export function getStoredAuthSession(): StoredAuthSession | null {
  if (!storageAvailable()) {
    return null
  }

  const accessToken = window.localStorage.getItem(ACCESS_TOKEN_KEY)
  const refreshToken = window.localStorage.getItem(REFRESH_TOKEN_KEY)
  const tokenType = window.localStorage.getItem(TOKEN_TYPE_KEY) || 'Bearer'
  const expiresAt = Number(window.localStorage.getItem(TOKEN_EXPIRES_AT_KEY) ?? '0')

  if (!accessToken || !refreshToken || !Number.isFinite(expiresAt) || expiresAt <= Date.now()) {
    clearAuthTokens()
    return null
  }

  return {
    accessToken,
    refreshToken,
    tokenType,
    expiresAt,
  }
}
