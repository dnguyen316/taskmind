import {
  describeNovaContext,
  type NovaChatContext,
  type NovaChatScope,
} from '../features/ai/composables/useNovaChat'

export interface AiAssistantWidgetStoryState {
  name: string
  selectedScope: NovaChatScope
  context: NovaChatContext
  expectedDisclosure: string
  expectedPayload: NovaChatContext
}

export const scopedChatStories: AiAssistantWidgetStoryState[] = [
  {
    name: 'scoped chat sends the current task and project',
    selectedScope: 'task',
    context: { scope: 'task', projectId: 'project-123', taskId: 'task-456' },
    expectedDisclosure: 'Nova can use this chat, the current task, and its project context.',
    expectedPayload: { scope: 'task', projectId: 'project-123', taskId: 'task-456' },
  },
  {
    name: 'reset chat clears local conversation state before a new session starts',
    selectedScope: 'project',
    context: { scope: 'project', projectId: 'project-123' },
    expectedDisclosure: 'Nova can use this chat and the current project workspace.',
    expectedPayload: { scope: 'project', projectId: 'project-123' },
  },
  {
    name: 'unavailable workspace context falls back to chat-only disclosure',
    selectedScope: 'none',
    context: { scope: 'none' },
    expectedDisclosure: 'Nova will only use messages in this chat. No workspace IDs are sent.',
    expectedPayload: { scope: 'none' },
  },
]

export function getScopedChatDisclosure(context: NovaChatContext) {
  return describeNovaContext(context)
}
