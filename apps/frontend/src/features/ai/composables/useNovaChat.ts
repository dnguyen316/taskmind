import { ref } from 'vue'
import { apiClient } from '../../../lib/apiClient'

export interface NovaChatMessage {
  role: 'user' | 'assistant'
  content: string
}

export function useNovaChat() {
  const messages = ref<NovaChatMessage[]>([])
  const loading = ref(false)
  async function send(message: string) {
    messages.value.push({ role: 'user', content: message })
    loading.value = true
    try {
      const response = await apiClient.post<{ message?: string; content?: string }>(
        '/v1/nova/chat',
        { message },
      )
      const content =
        response.data.message ?? response.data.content ?? 'Nova received your message.'
      messages.value.push({ role: 'assistant', content })
      return content
    } finally {
      loading.value = false
    }
  }
  return { messages, loading, send }
}
