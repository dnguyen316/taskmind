import { defineStore } from 'pinia'
import {
  clearAuthTokens,
  getStoredAuthSession,
  onAuthSessionStorageEvent,
  saveAuthTokens,
} from '../lib/authToken'
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
import { refreshAccessToken } from '../lib/apiClient'

interface JwtClaims {
  roles?: unknown
  permissions?: unknown
}

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
    roles: (state) => readTokenStringList(state.session?.accessToken, 'roles'),
    permissions: (state) => readTokenStringList(state.session?.accessToken, 'permissions'),
    canReadTeam(): boolean {
      return (
        hasAny(this.permissions, ['team.read', 'team.manage']) ||
        hasAnyRole(this.roles, ['ADMIN', 'MANAGER'])
      )
    },
    canManageTeam(): boolean {
      return (
        hasAny(this.permissions, ['team.manage', 'project.members.manage']) ||
        hasAnyRole(this.roles, ['ADMIN', 'MANAGER'])
      )
    },
    canManageGlobalRoles(): boolean {
      return hasAny(this.permissions, ['rbac.roles.manage']) || hasAnyRole(this.roles, ['ADMIN'])
    },
  },

  actions: {
    hasPermission(permission: string) {
      return this.permissions.includes(permission)
    },

    hasAnyPermission(permissions: string[]) {
      return permissions.some((permission) => this.hasPermission(permission))
    },

    hasRole(role: string) {
      return this.roles.some((existingRole) => existingRole.toUpperCase() === role.toUpperCase())
    },

    hasAnyRole(roles: string[]) {
      return roles.some((role) => this.hasRole(role))
    },

    async initializeSession() {
      this.session = getStoredAuthSession()

      if (!this.session) {
        try {
          const tokens = await refreshAccessToken()
          this.applyTokens(tokens)
        } catch {
          this.markUnauthenticated()
          return null
        }
      }

      try {
        await this.fetchCurrentUser({ silent: true })
      } catch {
        return null
      }

      this.initialized = true
      return this.session
    },

    async ensureInitialized() {
      if (!this.initialized) {
        await this.initializeSession()
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
      try {
        if (this.session) {
          await logoutSession()
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
        tokenType: tokens.tokenType || 'Bearer',
        expiresAt: Date.now() + tokens.expiresInSeconds * 1000,
      }
      this.initialized = true
    },

    applyStoredSession() {
      this.session = getStoredAuthSession()
      this.currentUser = this.session ? this.currentUser : null
      this.initialized = true
    },

    markAuthenticated() {
      this.session = getStoredAuthSession()
      this.currentUser = this.session ? this.currentUser : null
      this.initialized = true
    },

    watchSessionBroadcasts() {
      return onAuthSessionStorageEvent((type) => {
        if (type === 'cleared') {
          this.markUnauthenticated()
        } else {
          void this.initializeSession()
        }
      })
    },

    markUnauthenticated() {
      this.session = null
      this.currentUser = null
      this.pendingSignupEmail = ''
      this.initialized = true
    },
  },
})

function readTokenStringList(token: string | undefined, claim: keyof JwtClaims) {
  const claims = decodeJwtClaims(token)
  const value = claims?.[claim]

  if (!Array.isArray(value)) {
    return []
  }

  return value.filter((item): item is string => typeof item === 'string')
}

function decodeJwtClaims(token: string | undefined): JwtClaims | null {
  const payload = token?.split('.')[1]

  if (!payload) {
    return null
  }

  try {
    const normalizedPayload = payload.replace(/-/g, '+').replace(/_/g, '/')
    const paddedPayload = normalizedPayload.padEnd(
      normalizedPayload.length + ((4 - (normalizedPayload.length % 4)) % 4),
      '=',
    )
    return JSON.parse(window.atob(paddedPayload)) as JwtClaims
  } catch {
    return null
  }
}

function hasAny(values: string[], expected: string[]) {
  return expected.some((item) => values.includes(item))
}

function hasAnyRole(values: string[], expected: string[]) {
  return expected.some((role) =>
    values.some((existingRole) => existingRole.toUpperCase() === role.toUpperCase()),
  )
}
