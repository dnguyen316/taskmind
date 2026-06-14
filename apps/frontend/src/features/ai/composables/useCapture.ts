import { ref } from 'vue'
import { apiClient } from '../../../lib/apiClient'
import type { CaptureResponse } from './types'

export function useCapture() {
  const loading = ref(false)
  const result = ref<CaptureResponse | null>(null)
  async function capture(text: string) {
    loading.value = true
    try {
      const response = await apiClient.post<CaptureResponse>('/v1/ai/capture', { text })
      result.value = response.data
      return response.data
    } finally {
      loading.value = false
    }
  }
  return { loading, result, capture }
}
