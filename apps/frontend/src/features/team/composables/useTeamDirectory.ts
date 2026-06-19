import { computed, ref } from 'vue'
import { getTeamDirectory } from '../api/teamApi'
import type { TeamDirectory } from '../types'

export function useTeamDirectory() {
  const directory = ref<TeamDirectory | null>(null)
  const loading = ref(false)
  const errorMessage = ref('')

  const members = computed(() => directory.value?.members ?? [])
  const hasMembers = computed(() => members.value.length > 0)

  async function fetchDirectory() {
    loading.value = true
    errorMessage.value = ''

    try {
      directory.value = await getTeamDirectory()
      return directory.value
    } catch (error: unknown) {
      errorMessage.value = error instanceof Error ? error.message : 'Failed to load team directory.'
      throw error
    } finally {
      loading.value = false
    }
  }

  return { directory, members, hasMembers, loading, errorMessage, fetchDirectory }
}
