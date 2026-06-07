import { computed, ref } from 'vue'
import { searchActivity } from '../../tasks/api/activitySearchApi'
import type { ActivitySearchDocument } from '../../tasks/types'

export function useActivitySearch(defaultSize = 20) {
  const query = ref('')
  const size = ref(defaultSize)
  const loading = ref(false)
  const errorMessage = ref('')
  const results = ref<ActivitySearchDocument[]>([])

  const hasResults = computed(() => results.value.length > 0)

  async function runSearch() {
    loading.value = true
    errorMessage.value = ''

    try {
      results.value = await searchActivity({
        query: query.value,
        size: size.value,
      })
    } catch (error: unknown) {
      errorMessage.value = error instanceof Error ? error.message : 'Failed to search activity.'
    } finally {
      loading.value = false
    }
  }

  function clearSearch() {
    query.value = ''
    results.value = []
    errorMessage.value = ''
  }

  return {
    query,
    size,
    loading,
    errorMessage,
    results,
    hasResults,
    runSearch,
    clearSearch,
  }
}
