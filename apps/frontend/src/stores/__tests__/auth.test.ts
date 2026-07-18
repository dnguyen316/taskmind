import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { clearAuthTokens, getAccessToken } from '../../lib/authToken'
import { refreshAccessToken } from '../../lib/apiClient'
import { getCurrentUser } from '../../features/auth/api/authApi'
import { useAuthStore } from '../auth'

vi.mock('../../lib/apiClient', () => ({
  refreshAccessToken: vi.fn(),
}))

vi.mock('../../features/auth/api/authApi', () => ({
  getCurrentUser: vi.fn(),
  login: vi.fn(),
  logout: vi.fn(),
  signupEmail: vi.fn(),
  verifyOtp: vi.fn(),
}))

const refreshAccessTokenMock = vi.mocked(refreshAccessToken)
const getCurrentUserMock = vi.mocked(getCurrentUser)

describe('auth store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    clearAuthTokens()
    vi.clearAllMocks()
  })

  it('clears refreshed tokens and initializes when current user lookup fails', async () => {
    refreshAccessTokenMock.mockResolvedValue({
      accessToken: 'fresh-access-token',
      tokenType: 'Bearer',
      expiresInSeconds: 3600,
    })
    getCurrentUserMock.mockRejectedValue(new Error('profile unavailable'))

    const authStore = useAuthStore()

    const session = await authStore.initializeSession()

    expect(session).toBeNull()
    expect(authStore.session).toBeNull()
    expect(authStore.currentUser).toBeNull()
    expect(authStore.initialized).toBe(true)
    expect(getAccessToken()).toBeNull()
  })
})
