import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { getAccessToken, getTokenExpiresAt } from '../lib/authToken'

export const E2E_AUTH_CREDENTIALS = {
  email: 'superadmin@taskmind.local',
  password: '1',
  otp: '1',
} as const

export const useAuthStore = defineStore('auth', () => {
  const initialized = ref(false)
  const authenticated = ref(false)

  const isAuthenticated = computed(() => authenticated.value)

  async function ensureInitialized() {
    if (initialized.value) {
      return
    }

    const accessToken = getAccessToken()
    const expiresAt = getTokenExpiresAt()
    authenticated.value = Boolean(accessToken && (!expiresAt || expiresAt > Date.now()))
    initialized.value = true
  }

  function markAuthenticated() {
    authenticated.value = true
    initialized.value = true
  }

  function markUnauthenticated() {
    authenticated.value = false
    initialized.value = true
  }

  return {
    E2E_AUTH_CREDENTIALS,
    initialized,
    isAuthenticated,
    ensureInitialized,
    markAuthenticated,
    markUnauthenticated,
  }
})
