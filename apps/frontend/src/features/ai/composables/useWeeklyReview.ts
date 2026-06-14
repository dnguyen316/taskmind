import { ref } from 'vue'
import { apiClient } from '../../../lib/apiClient'
import type { WeeklyReviewResponse } from './types'

export function useWeeklyReview() {
  const loading = ref(false)
  const result = ref<WeeklyReviewResponse | null>(null)
  async function generate(userId: string) {
    loading.value = true
    try {
      const response = await apiClient.post<WeeklyReviewResponse>('/v1/review/weekly/generate', {
        userId,
      })
      result.value = response.data
      return response.data
    } finally {
      loading.value = false
    }
  }
  return { loading, result, generate }
}
