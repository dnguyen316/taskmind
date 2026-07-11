const TOKEN_EXPIRY_SKEW_MS = 30_000
const SESSION_EVENT_KEY = 'taskmind.authSessionEvent'

export interface StoredAuthTokens {
  accessToken: string
  tokenType: string
  expiresInSeconds: number
}

export interface StoredAuthSession {
  accessToken: string
  tokenType: string
  expiresAt: number
}

let accessToken: string | null = null
let tokenType = 'Bearer'
let expiresAt: number | null = null

function storageAvailable() {
  return typeof window !== 'undefined' && typeof window.localStorage !== 'undefined'
}

function broadcastSessionEvent(type: 'authenticated' | 'refreshed' | 'cleared') {
  if (!storageAvailable()) return
  try {
    window.localStorage.setItem(SESSION_EVENT_KEY, JSON.stringify({ type, at: Date.now() }))
  } catch {
    // Ignore broadcast failures; auth state is still updated in this tab.
  }
}

export function saveAuthTokens(tokens: StoredAuthTokens) {
  tokenType = tokens.tokenType || 'Bearer'
  accessToken = tokens.accessToken
  expiresAt = Date.now() + tokens.expiresInSeconds * 1000
  broadcastSessionEvent('authenticated')
}

export function clearAuthTokens() {
  accessToken = null
  tokenType = 'Bearer'
  expiresAt = null
  broadcastSessionEvent('cleared')
}

export function getAccessToken() {
  return accessToken
}

export function getTokenExpiresAt() {
  return expiresAt
}

export function isAccessTokenExpired(now = Date.now()) {
  return expiresAt !== null && expiresAt <= now
}

export function isAccessTokenExpiringSoon(now = Date.now(), skewMs = TOKEN_EXPIRY_SKEW_MS) {
  return expiresAt !== null && expiresAt <= now + skewMs
}

export function getAuthorizationHeader() {
  if (!accessToken) return null
  return `${tokenType} ${accessToken}`
}

export function getStoredAuthSession(): StoredAuthSession | null {
  if (!accessToken || !expiresAt || expiresAt <= Date.now()) {
    clearAuthTokens()
    return null
  }
  return { accessToken, tokenType, expiresAt }
}

export function markTokensRefreshed() {
  broadcastSessionEvent('refreshed')
}

export function onAuthSessionStorageEvent(handler: (type: string) => void) {
  if (typeof window === 'undefined') return () => {}
  const listener = (event: StorageEvent) => {
    if (event.key !== SESSION_EVENT_KEY || !event.newValue) return
    try {
      const payload = JSON.parse(event.newValue) as { type?: string }
      if (payload.type) handler(payload.type)
    } catch {
      // Ignore malformed session broadcasts.
    }
  }
  window.addEventListener('storage', listener)
  return () => window.removeEventListener('storage', listener)
}
