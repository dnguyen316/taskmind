import { computed } from 'vue'
import { useAuthStore } from '../stores/auth'

export function useCurrentUserId() {
  const authStore = useAuthStore()
  const currentUserId = computed(() => authStore.currentUserId)

  function requireCurrentUserId() {
    if (!authStore.currentUserId) {
      throw new Error('Your profile is still loading. Please wait a moment and try again.')
    }

    return authStore.currentUserId
  }

  return {
    currentUserId,
    requireCurrentUserId,
  }
}
