import { computed, onScopeDispose, ref, watch } from 'vue'
import {
  assistActivitySearch,
  searchActivity,
  suggestActivitySearch,
} from '../../tasks/api/activitySearchApi'
import type { ActivitySearchAssistResponse, ActivitySearchDocument } from '../../tasks/types'

export function useActivitySearch(defaultSize = 20) {
  const query = ref('')
  const size = ref(defaultSize)
  const loading = ref(false)
  const errorMessage = ref('')
  const results = ref<ActivitySearchDocument[]>([])
  const suggestions = ref<string[]>([])
  const suggestionsLoading = ref(false)
  const suggestionsErrorMessage = ref('')
  const aiLoading = ref(false)
  const aiErrorMessage = ref('')
  const aiProposal = ref<ActivitySearchAssistResponse | null>(null)
  let suggestionTimer: ReturnType<typeof setTimeout> | undefined
  let suggestionRequestId = 0

  const hasResults = computed(() => results.value.length > 0)

  async function loadSuggestions() {
    const suggestionQuery = query.value.trim()
    suggestionRequestId += 1
    const requestId = suggestionRequestId

    if (suggestionQuery.length < 2) {
      suggestions.value = []
      suggestionsErrorMessage.value = ''
      suggestionsLoading.value = false
      return
    }

    suggestionsLoading.value = true
    suggestionsErrorMessage.value = ''

    try {
      const nextSuggestions = await suggestActivitySearch({ query: suggestionQuery, size: 8 })
      if (requestId === suggestionRequestId) {
        suggestions.value = nextSuggestions
      }
    } catch (error: unknown) {
      if (requestId === suggestionRequestId) {
        suggestions.value = []
        suggestionsErrorMessage.value =
          error instanceof Error ? error.message : 'Failed to load activity suggestions.'
      }
    } finally {
      if (requestId === suggestionRequestId) {
        suggestionsLoading.value = false
      }
    }
  }

  watch(query, () => {
    if (suggestionTimer) {
      clearTimeout(suggestionTimer)
    }
    suggestionTimer = setTimeout(() => {
      void loadSuggestions()
    }, 250)
  })

  onScopeDispose(() => {
    if (suggestionTimer) {
      clearTimeout(suggestionTimer)
    }
  })

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
    suggestions.value = []
    suggestionsErrorMessage.value = ''
    aiProposal.value = null
    aiErrorMessage.value = ''
  }

  return {
    query,
    size,
    loading,
    errorMessage,
    results,
    suggestions,
    suggestionsLoading,
    suggestionsErrorMessage,
    aiLoading,
    aiErrorMessage,
    aiProposal,
    hasResults,
    loadSuggestions,
    askNova,
    applyAiProposal,
    dismissAiProposal,
    runSearch,
    clearSearch,
  }
}
