import { computed, ref } from 'vue'
import { assistActivitySearch, searchActivity } from '../../tasks/api/activitySearchApi'
import type { ActivitySearchAssistResponse, ActivitySearchDocument } from '../../tasks/types'

export function useActivitySearch(defaultSize = 20) {
  const query = ref('')
  const size = ref(defaultSize)
  const entityType = ref('')
  const status = ref('')
  const projectId = ref('')
  const from = ref('')
  const to = ref('')
  const eventType = ref('')
  const loading = ref(false)
  const errorMessage = ref('')
  const results = ref<ActivitySearchDocument[]>([])
  const aiLoading = ref(false)
  const aiErrorMessage = ref('')
  const aiProposal = ref<ActivitySearchAssistResponse | null>(null)

  const hasResults = computed(() => results.value.length > 0)

  function currentFilters() {
    return {
      entityType: entityType.value,
      status: status.value,
      projectId: projectId.value,
      from: from.value ? new Date(from.value).toISOString() : undefined,
      to: to.value ? new Date(to.value).toISOString() : undefined,
      eventType: eventType.value,
    }
  }

  async function askNova() {
    const prompt = query.value.trim()
    if (!prompt) {
      aiErrorMessage.value = 'Enter a search intent before asking Nova.'
      return
    }

    aiLoading.value = true
    aiErrorMessage.value = ''
    aiProposal.value = null

    try {
      aiProposal.value = await assistActivitySearch(prompt, query.value)
    } catch (error: unknown) {
      aiErrorMessage.value =
        error instanceof Error ? error.message : 'Nova could not refine this search.'
    } finally {
      aiLoading.value = false
    }
  }

  function applyAiProposal() {
    if (!aiProposal.value) {
      return
    }
    query.value = aiProposal.value.query
    aiProposal.value = null
    void runSearch()
  }

  function dismissAiProposal() {
    aiProposal.value = null
    aiErrorMessage.value = ''
  }

  async function runSearch() {
    loading.value = true
    errorMessage.value = ''

    try {
      results.value = await searchActivity({
        query: query.value,
        size: size.value,
        ...currentFilters(),
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
    entityType.value = ''
    status.value = ''
    projectId.value = ''
    from.value = ''
    to.value = ''
    eventType.value = ''
    aiProposal.value = null
    aiErrorMessage.value = ''
  }

  return {
    query,
    size,
    entityType,
    status,
    projectId,
    from,
    to,
    eventType,
    loading,
    errorMessage,
    results,
    aiLoading,
    aiErrorMessage,
    aiProposal,
    hasResults,
    askNova,
    applyAiProposal,
    dismissAiProposal,
    runSearch,
    clearSearch,
  }
}
