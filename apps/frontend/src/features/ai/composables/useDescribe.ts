import { ref } from 'vue'
import { apiClient } from '../../../lib/apiClient'
import type { AutocompleteResponse, DescribeTaskResponse } from './types'

export function useDescribe() {
  const loading = ref(false)
  async function describe(title: string, notes?: string) {
    loading.value = true
    try {
      return (await apiClient.post<DescribeTaskResponse>('/v1/ai/tasks/describe', { title, notes }))
        .data
    } finally {
      loading.value = false
    }
  }
  async function autocomplete(text: string) {
    return (
      await apiClient.post<AutocompleteResponse>('/v1/ai/tasks/describe/autocomplete', { text })
    ).data
  }
  return { loading, describe, autocomplete }
}
