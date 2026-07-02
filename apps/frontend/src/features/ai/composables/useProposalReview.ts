import { apiClient } from '../../../lib/apiClient'

export type AiProposalActionType =
  | 'CREATE_TASK'
  | 'UPDATE_TASK'
  | 'SCHEDULE_TASK'
  | 'CREATE_PROJECT'
  | 'ADD_SUBTASKS'
  | 'ARCHIVE_TASK'
  | 'ASSIGN_TASK'

export type AiProposalStatus = 'PENDING' | 'ACCEPTED' | 'EDITED' | 'REJECTED' | 'EXPIRED'
export type AiProposalSource =
  | 'AI_CAPTURE'
  | 'GOAL_BREAKDOWN'
  | 'SPEC_BREAKDOWN'
  | 'SCHEDULER'
  | 'NOVA_CHAT'
  | 'TASK_RESOLUTION'

export interface AiProposal {
  id: string
  userId: string
  actionType: AiProposalActionType
  status: AiProposalStatus
  proposedPayload: unknown
  preview: string
  rationale: string | null
  proposer: string
  provider: string | null
  model: string | null
  source: AiProposalSource
  sourceContext: string | null
  createdAt: string
  expiresAt: string | null
  acceptedAt: string | null
  decidedBy: string | null
  userDecision: string | null
}

export interface AiProposalImpactPreview {
  proposalId: string
  actionType: AiProposalActionType
  summary: string
  affectedResources: string[]
  proposedPayload: unknown
}

export async function listPendingAiProposals(): Promise<AiProposal[]> {
  return (await apiClient.get<AiProposal[]>('/v1/ai/proposals/pending')).data
}

export async function previewAiProposal(proposalId: string): Promise<AiProposalImpactPreview> {
  return (await apiClient.get<AiProposalImpactPreview>(`/v1/ai/proposals/${proposalId}/preview`))
    .data
}

export async function acceptAiProposal(proposalId: string): Promise<AiProposal> {
  return (await apiClient.post<AiProposal>(`/v1/ai/proposals/${proposalId}/accept`)).data
}

export async function rejectAiProposal(proposalId: string, reason?: string): Promise<AiProposal> {
  return (await apiClient.post<AiProposal>(`/v1/ai/proposals/${proposalId}/reject`, { reason }))
    .data
}

export async function acceptAiProposalWithEdits(
  proposalId: string,
  editedPayload: unknown,
): Promise<AiProposal> {
  return (
    await apiClient.post<AiProposal>(`/v1/ai/proposals/${proposalId}/accept-with-edits`, {
      editedPayload,
    })
  ).data
}
