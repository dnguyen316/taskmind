export interface CapturedTaskDraft {
  title: string
  status: string
  priority: number
  durationMinutes: number
  source: string
  confidence: number
}
export interface CaptureResponse {
  drafts: CapturedTaskDraft[]
  clarificationQuestion: string | null
}
export interface GoalBreakdownResponse {
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
export interface WeeklyReviewResponse {
  userId: string
  summary: string
  slippageInsights: string[]
  recommendations: string[]
  nextWeekPriorities: string[]
}
export interface DescribeTaskResponse {
  description: string
  rationale: string
}
export interface AutocompleteResponse {
  suggestions: string[]
}
export interface TranslateTaskResponse {
  translatedText: string
  targetLanguage: string
}
