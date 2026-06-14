import { defineStore } from 'pinia'
import { clearAuthTokens, getStoredAuthSession, saveAuthTokens } from '../lib/authToken'
import {
  getCurrentUser,
  login,
  logout as logoutSession,
  signupEmail,
  verifyOtp,
} from '../features/auth/api/authApi'
import type {
  AuthTokensResponse,
  AuthUserResponse,
  LoginPayload,
  SignupEmailPayload,
  VerifyOtpPayload,
} from '../features/auth/api/authApi'
import type { StoredAuthSession } from '../lib/authToken'

export const E2E_AUTH_CREDENTIALS = {
  email: 'superadmin@taskmind.local',
  password: '1',
  otp: '1',
} as const

type AuthMode = 'login' | 'signup'

interface FetchCurrentUserOptions {
  silent?: boolean
}

interface AuthState {
  session: StoredAuthSession | null
  currentUser: AuthUserResponse | null
  initialized: boolean
  isSubmitting: boolean
  isLoadingProfile: boolean
  errorMessage: string
  pendingSignupEmail: string
}

export const useAuthStore = defineStore('auth', {
  state: (): AuthState => ({
    session: null,
    currentUser: null,
    initialized: false,
    isSubmitting: false,
    isLoadingProfile: false,
    errorMessage: '',
    pendingSignupEmail: '',
  }),

  getters: {
    isAuthenticated: (state) => Boolean(state.session?.accessToken),
    currentUserId: (state) => state.currentUser?.userId ?? '',
    currentUserDisplayName: (state) =>
      state.currentUser?.displayName || state.currentUser?.email || 'TaskMind user',
  },

  actions: {
    initializeSession() {
      this.session = getStoredAuthSession()
      this.currentUser = this.session ? this.currentUser : null
      this.initialized = true
      return this.session
    },

    async ensureInitialized() {
      if (!this.initialized) {
        this.initializeSession()
      }

      if (this.session && !this.currentUser) {
        await this.fetchCurrentUser({ silent: true })
      }
    },

    async authenticate(mode: AuthMode, payload: LoginPayload | SignupEmailPayload) {
      if (mode === 'signup') {
        await this.requestSignup(payload as SignupEmailPayload)
        return null
      }

      return this.loginWithPassword(payload)
    },

    async loginWithPassword(payload: LoginPayload) {
      this.errorMessage = ''
      this.isSubmitting = true

      try {
        const tokens = await login(payload)
        this.applyTokens(tokens)
        await this.fetchCurrentUser()
        return this.session
      } catch (error: unknown) {
        this.errorMessage =
          error instanceof Error ? error.message : 'Authentication failed. Please try again.'
        throw error
      } finally {
        this.isSubmitting = false
      }
    },

    async requestSignup(payload: SignupEmailPayload) {
      this.errorMessage = ''
      this.isSubmitting = true

      try {
        await signupEmail(payload)
        this.pendingSignupEmail = payload.email.trim()
      } catch (error: unknown) {
        this.errorMessage =
          error instanceof Error ? error.message : 'Signup failed. Please try again.'
        throw error
      } finally {
        this.isSubmitting = false
      }
    },

    async verifySignup(payload: VerifyOtpPayload) {
      this.errorMessage = ''
      this.isSubmitting = true

      try {
        const tokens = await verifyOtp(payload)
        this.pendingSignupEmail = ''
        this.applyTokens(tokens)
        await this.fetchCurrentUser()
        return this.session
      } catch (error: unknown) {
        this.errorMessage =
          error instanceof Error ? error.message : 'Verification failed. Please try again.'
        throw error
      } finally {
        this.isSubmitting = false
      }
    },

    async fetchCurrentUser({ silent = false }: FetchCurrentUserOptions = {}) {
      if (!this.session) {
        this.currentUser = null
        return null
      }

      this.isLoadingProfile = true

      try {
        this.currentUser = await getCurrentUser()
        return this.currentUser
      } catch (error: unknown) {
        clearAuthTokens()
        this.markUnauthenticated()
        if (!silent) {
          this.errorMessage =
            error instanceof Error
              ? error.message
              : 'Your session has expired. Please sign in again.'
        }
        throw error
      } finally {
        this.isLoadingProfile = false
      }
    },

    async logout() {
      const refreshToken = this.session?.refreshToken

      try {
        if (refreshToken) {
          await logoutSession({ refreshToken })
        }
      } finally {
        clearAuthTokens()
        this.markUnauthenticated()
        this.errorMessage = ''
      }
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

    markAuthenticated() {
      this.session = getStoredAuthSession()
      this.currentUser = this.session ? this.currentUser : null
      this.initialized = true
    },

    markUnauthenticated() {
      this.session = null
      this.currentUser = null
      this.pendingSignupEmail = ''
      this.initialized = true
    },
  },
})
