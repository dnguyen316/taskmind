import { computed, ref, type MaybeRefOrGetter, toValue } from 'vue'
import { apiClient, fetchWithAuthRetry } from '../../../lib/apiClient'
import { isApiError } from '../../../lib/apiError'
import { getAuthorizationHeader } from '../../../lib/authToken'

export type NovaChatScope = 'task' | 'project' | 'visible' | 'none'

export interface NovaChatContext {
  projectId?: string | null
  taskId?: string | null
  scope?: NovaChatScope
}

export interface NovaChatMessage {
  role: 'user' | 'assistant'
  content: string
}

interface NovaChatStreamChunk {
  sessionId?: string
  content?: string
  done?: boolean
}

export function describeNovaContext(context: NovaChatContext) {
  if (context.scope === 'none') {
    return 'Nova will only use messages in this chat. No workspace IDs are sent.'
  }
  if (context.scope === 'task' && context.taskId) {
    return 'Nova can use this chat, the current task, and its project context.'
  }
  if (context.scope === 'project' && context.projectId) {
    return 'Nova can use this chat and the current project workspace.'
  }
  if (context.scope === 'visible') {
    return 'Nova can use this chat and the work visible on this page.'
  }
  return 'No workspace context is available on this page. Nova will only use this chat.'
}

export function useNovaChat(context: MaybeRefOrGetter<NovaChatContext> = {}) {
  const messages = ref<NovaChatMessage[]>([])
  const loading = ref(false)
  const errorMessage = ref<string | null>(null)
  const sessionId = ref<string | null>(null)
  const activeContext = computed(() => normalizeContext(toValue(context)))

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

  function resetChat() {
    messages.value = []
    sessionId.value = null
    errorMessage.value = null
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
        body: JSON.stringify({ message, sessionId: sessionId.value, ...activeContext.value }),
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

  return { messages, loading, errorMessage, sessionId, activeContext, resetChat, send }
}

function normalizeContext(context: NovaChatContext): NovaChatContext {
  const scope = context.scope ?? 'none'
  if (scope === 'none') {
    return { scope: 'none' }
  }
  return {
    scope,
    ...(context.projectId ? { projectId: context.projectId } : {}),
    ...(context.taskId ? { taskId: context.taskId } : {}),
  }
}
