import { ref } from 'vue'
import { apiClient } from '../../../lib/apiClient'
import { isApiError } from '../../../lib/apiError'

export interface NovaChatMessage {
  role: 'user' | 'assistant'
  content: string
}

export function useNovaChat() {
  const messages = ref<NovaChatMessage[]>([])
  const loading = ref(false)
  const errorMessage = ref<string | null>(null)
  async function send(message: string) {
    messages.value.push({ role: 'user', content: message })
    loading.value = true
    errorMessage.value = null
    try {
      const response = await apiClient.post<{ message?: string; content?: string }>(
        '/v1/nova/chat',
        { message },
      )
      const content =
        response.data.message ?? response.data.content ?? 'Nova received your message.'
      messages.value.push({ role: 'assistant', content })
      return content
    } catch (error) {
      errorMessage.value = isApiError(error)
        ? error.message
        : 'Nova could not answer right now. Please try again.'
    } finally {
      loading.value = false
    }
  }
  return { messages, loading, errorMessage, send }
}
