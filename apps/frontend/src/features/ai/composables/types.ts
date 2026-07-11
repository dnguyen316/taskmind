import type { TaskStatus } from '../../tasks/types'

export type AiResponseSource = 'NOVA' | 'LOCAL_FALLBACK'

export interface AiResponseMetadata {
  source: AiResponseSource
  degraded: boolean
}

export interface CapturedTaskDraft {
  title: string
  status: TaskStatus
  priority: number
  durationMinutes: number
  source: string
  confidence: number
}
export interface CaptureResponse extends AiResponseMetadata {
  drafts: CapturedTaskDraft[]
  clarificationQuestion: string | null
}
export interface CaptureAcceptRequest {
  draft: CapturedTaskDraft
  projectId?: string | null
  assigneeId?: string | null
  parentTaskId?: string | null
  description?: string | null
  dueAt?: string | null
}
export interface CaptureAcceptResponse {
  taskId: string
}
export interface CaptureRejectRequest {
  draft: CapturedTaskDraft
  reason: string
}
export interface CaptureRejectResponse {
  rejected: boolean
}
export interface GoalBreakdownResponse extends AiResponseMetadata {
  goalId: string
  milestones: Array<{ title: string; targetDate: string | null; notes: string[] }>
  tasks: Array<{
    title: string
    status: string
    dueAt: string | null
    rationale: string
    dependencies: string[]
  }>
  riskNotes: string[]
}
export interface WeeklyReviewResponse extends AiResponseMetadata {
  userId: string
  summary: string
  slippageInsights: string[]
  recommendations: string[]
  nextWeekPriorities: string[]
}
export interface DescribeTaskResponse extends AiResponseMetadata {
  description: string
  rationale: string
}
export interface AutocompleteResponse extends AiResponseMetadata {
  suggestions: string[]
}
export interface TranslateTaskResponse extends AiResponseMetadata {
  translatedText: string
  targetLanguage: string
}
