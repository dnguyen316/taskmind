import { defineStore } from 'pinia'
import { clearAuthTokens, getStoredAuthSession, saveAuthTokens } from '../lib/authToken'
import { login, signupEmail } from '../features/auth/api/authApi'
import type { AuthTokensResponse, LoginPayload, SignupEmailPayload } from '../features/auth/api/authApi'

type AuthMode = 'login' | 'signup'

interface AuthSession {
  accessToken: string
  refreshToken: string
  tokenType: string
  expiresAt: number
}

interface AuthState {
  session: AuthSession | null
  initialized: boolean
  isSubmitting: boolean
  errorMessage: string
}

export const useAuthStore = defineStore('auth', {
  state: (): AuthState => ({
    session: null,
    initialized: false,
    isSubmitting: false,
    errorMessage: '',
  }),

  getters: {
    isAuthenticated: (state) => Boolean(state.session?.accessToken),
  },

  actions: {
    initializeSession() {
      this.session = getStoredAuthSession()
      this.initialized = true
      return this.session
    },

    async authenticate(mode: AuthMode, payload: LoginPayload | SignupEmailPayload) {
      this.errorMessage = ''
      this.isSubmitting = true

      try {
        const tokens = mode === 'signup' ? await signupEmail(payload as SignupEmailPayload) : await login(payload)
        this.applyTokens(tokens)
        return this.session
      } catch (error: unknown) {
        this.errorMessage = error instanceof Error ? error.message : 'Authentication failed. Please try again.'
        throw error
      } finally {
        this.isSubmitting = false
      }
    },

    logout() {
      clearAuthTokens()
      this.session = null
      this.initialized = true
      this.errorMessage = ''
    },

    applyTokens(tokens: AuthTokensResponse) {
      saveAuthTokens(tokens)
      this.session = {
        accessToken: tokens.accessToken,
        refreshToken: tokens.refreshToken,
        tokenType: tokens.tokenType || 'Bearer',
        expiresAt: Date.now() + tokens.expiresInSeconds * 1000,
      }
      this.initialized = true
    },
  },
})
