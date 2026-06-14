import { ref } from 'vue'
import { apiClient } from '../../../lib/apiClient'
import type { GoalBreakdownResponse } from './types'

export function useGoalBreakdown() {
  const loading = ref(false)
  const result = ref<GoalBreakdownResponse | null>(null)
  async function breakdown(
    goalId: string,
    payload: { deadline?: string | null; weeklyAvailabilityMinutes?: number | null },
  ) {
    loading.value = true
    try {
      const response = await apiClient.post<GoalBreakdownResponse>(
        `/v1/ai/goals/${goalId}/breakdown`,
        payload,
      )
      result.value = response.data
      return response.data
    } finally {
      loading.value = false
    }
  }
  return { loading, result, breakdown }
}
