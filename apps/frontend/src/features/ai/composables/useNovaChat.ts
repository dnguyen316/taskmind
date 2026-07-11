import { ref } from 'vue'
import { apiClient, fetchWithAuthRetry } from '../../../lib/apiClient'
import { isApiError } from '../../../lib/apiError'
import { getAuthorizationHeader } from '../../../lib/authToken'

export interface NovaChatMessage {
  role: 'user' | 'assistant'
  content: string
}

interface NovaChatStreamChunk {
  sessionId?: string
  content?: string
  done?: boolean
}

export function useNovaChat() {
  const messages = ref<NovaChatMessage[]>([])
  const loading = ref(false)
  const errorMessage = ref<string | null>(null)
  const sessionId = ref<string | null>(null)

  async function send(message: string) {
    messages.value.push({ role: 'user', content: message })
    const assistantMessage: NovaChatMessage = { role: 'assistant', content: '' }
    messages.value.push(assistantMessage)
    loading.value = true
    errorMessage.value = null

    try {
      const content = await streamChat(message, assistantMessage)
      if (!content) {
        assistantMessage.content = 'Nova received your message.'
      }
      return assistantMessage.content
    } catch (error) {
      messages.value = messages.value.filter((entry) => entry !== assistantMessage)
      errorMessage.value = isApiError(error)
        ? error.message
        : error instanceof Error
          ? error.message
          : 'Nova could not answer right now. Please try again.'
    } finally {
      loading.value = false
    }
  }

  async function streamChat(message: string, assistantMessage: NovaChatMessage) {
    const response = await fetchWithAuthRetry(() => {
      const authorizationHeader = getAuthorizationHeader()

      return fetch(`${apiClient.defaults.baseURL ?? ''}/v1/nova/chat/stream`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Accept: 'text/event-stream',
          ...(authorizationHeader ? { Authorization: authorizationHeader } : {}),
        },
        credentials: 'include',
        body: JSON.stringify({ message, sessionId: sessionId.value }),
      })
    })

    if (!response.ok) {
      throw new Error(await readErrorMessage(response))
    }

    if (!response.body) {
      return assistantMessage.content
    }

    const reader = response.body.getReader()
    const decoder = new TextDecoder()
    let buffer = ''

    while (true) {
      const { done, value } = await reader.read()
      if (done) {
        break
      }
      buffer += decoder.decode(value, { stream: true })
      buffer = consumeSseBuffer(buffer, assistantMessage)
    }

    buffer += decoder.decode()
    consumeSseBuffer(`${buffer}\n\n`, assistantMessage)
    return assistantMessage.content
  }

  function consumeSseBuffer(buffer: string, assistantMessage: NovaChatMessage) {
    const events = buffer.split(/\n\n/)
    const remainder = events.pop() ?? ''

    for (const event of events) {
      const data = event
        .split(/\n/)
        .filter((line) => line.startsWith('data:'))
        .map((line) => line.slice(5).trimStart())
        .join('\n')

      if (!data || data === '[DONE]') {
        continue
      }

      const chunk = JSON.parse(data) as NovaChatStreamChunk
      if (chunk.sessionId) {
        sessionId.value = chunk.sessionId
      }
      if (chunk.content) {
        assistantMessage.content += chunk.content
      }
    }

    return remainder
  }

  async function readErrorMessage(response: Response) {
    try {
      const body = (await response.json()) as { detail?: string; message?: string; title?: string }
      return (
        body.detail ??
        body.message ??
        body.title ??
        'Nova could not answer right now. Please try again.'
      )
    } catch {
      return 'Nova could not answer right now. Please try again.'
    }
  }

  return { messages, loading, errorMessage, sessionId, send }
}
