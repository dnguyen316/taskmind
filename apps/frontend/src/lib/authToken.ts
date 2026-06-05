const ACCESS_TOKEN_KEY = 'taskmind.accessToken'
const REFRESH_TOKEN_KEY = 'taskmind.refreshToken'
const TOKEN_TYPE_KEY = 'taskmind.tokenType'
const TOKEN_EXPIRES_AT_KEY = 'taskmind.tokenExpiresAt'
const TOKEN_EXPIRY_SKEW_MS = 30_000

export interface StoredAuthTokens {
  accessToken: string
  refreshToken: string
  tokenType: string
  expiresInSeconds: number
}

function storageAvailable() {
  return typeof window !== 'undefined' && typeof window.localStorage !== 'undefined'
}

function safeStorageRead(key: string) {
  if (!storageAvailable()) {
    return null
  }

  try {
    return window.localStorage.getItem(key)
  } catch {
    return null
  }
}

function safeStorageWrite(key: string, value: string) {
  if (!storageAvailable()) {
    return
  }

  try {
    window.localStorage.setItem(key, value)
  } catch {
    clearAuthTokens()
  }
}

function safeStorageRemove(key: string) {
  if (!storageAvailable()) {
    return
  }

  try {
    window.localStorage.removeItem(key)
  } catch {
    // Ignore storage failures so logout/session-expiry cleanup stays best-effort.
  }
}

export function saveAuthTokens(tokens: StoredAuthTokens) {
  const tokenType = tokens.tokenType || 'Bearer'
  const expiresAt = Date.now() + tokens.expiresInSeconds * 1000

  safeStorageWrite(ACCESS_TOKEN_KEY, tokens.accessToken)
  safeStorageWrite(REFRESH_TOKEN_KEY, tokens.refreshToken)
  safeStorageWrite(TOKEN_TYPE_KEY, tokenType)
  safeStorageWrite(TOKEN_EXPIRES_AT_KEY, String(expiresAt))
}

export function clearAuthTokens() {
  safeStorageRemove(ACCESS_TOKEN_KEY)
  safeStorageRemove(REFRESH_TOKEN_KEY)
  safeStorageRemove(TOKEN_TYPE_KEY)
  safeStorageRemove(TOKEN_EXPIRES_AT_KEY)
}

export function getAccessToken() {
  return safeStorageRead(ACCESS_TOKEN_KEY)
}

export function getRefreshToken() {
  return safeStorageRead(REFRESH_TOKEN_KEY)
}

export function getTokenExpiresAt() {
  const expiresAt = safeStorageRead(TOKEN_EXPIRES_AT_KEY)

  if (!expiresAt) {
    return null
  }

  const parsedExpiresAt = Number(expiresAt)
  return Number.isFinite(parsedExpiresAt) ? parsedExpiresAt : null
}

export function isAccessTokenExpired(now = Date.now()) {
  const expiresAt = getTokenExpiresAt()
  return expiresAt !== null && expiresAt <= now
}

export function isAccessTokenExpiringSoon(now = Date.now(), skewMs = TOKEN_EXPIRY_SKEW_MS) {
  const expiresAt = getTokenExpiresAt()
  return expiresAt !== null && expiresAt <= now + skewMs
}

export function getAuthorizationHeader() {
  const accessToken = getAccessToken()

  if (!accessToken) {
    return null
  }

  const tokenType = safeStorageRead(TOKEN_TYPE_KEY) || 'Bearer'
  return `${tokenType} ${accessToken}`
}
