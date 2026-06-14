import { ref } from 'vue'
import { apiClient } from '../../../lib/apiClient'
import type { TranslateTaskResponse } from './types'

export function useTranslate() {
  const loading = ref(false)
  async function translate(text: string, targetLanguage: string) {
    loading.value = true
    try {
      return (
        await apiClient.post<TranslateTaskResponse>('/v1/ai/tasks/translate', {
          text,
          targetLanguage,
        })
      ).data
    } finally {
      loading.value = false
    }
  }
  return { loading, translate }
}
