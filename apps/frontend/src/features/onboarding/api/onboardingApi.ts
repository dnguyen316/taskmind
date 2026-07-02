import { apiClient } from '../../../lib/apiClient'

export type WorkspaceType = 'TEAM' | 'SOLO' | 'CLIENT'
export type PlanningStyle = 'SPRINT' | 'KANBAN' | 'TIME_BLOCKING'
export type StartMode = 'BLANK' | 'TEMPLATE' | 'SPEC' | 'GITHUB' | 'JIRA' | 'DEMO'

export interface OnboardingTemplate {
  key: string
  name: string
  description: string
}

export interface CompleteOnboardingPayload {
  workspaceType: WorkspaceType
  planningStyle: PlanningStyle
  startMode: StartMode
  templateKey?: string
}

export interface OnboardingResult {
  onboardingCompleted: boolean
  projectId: string
}

export async function listOnboardingTemplates() {
  const response = await apiClient.get<OnboardingTemplate[]>('/v1/onboarding/templates')
  return response.data
}

export async function completeOnboarding(payload: CompleteOnboardingPayload) {
  const response = await apiClient.post<OnboardingResult>('/v1/onboarding/complete', payload)
  return response.data
}

export async function resetDemoWorkspace() {
  const response = await apiClient.post<OnboardingResult>('/v1/onboarding/demo/reset')
  return response.data
}
