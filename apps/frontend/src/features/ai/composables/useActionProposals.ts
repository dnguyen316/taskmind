import { apiClient } from '../../../lib/apiClient'

export type AiActionProposalStatus = 'PENDING' | 'APPROVED' | 'REJECTED' | 'EXECUTED' | 'FAILED'

export interface AiActionProposal {
  id: string
  jobId: string
  proposedActionType: string
  payloadPreview: string
  riskLevel: string
  rationale: string | null
  status: AiActionProposalStatus
  decidedBy: string | null
  decidedAt: string | null
  errorCode: string | null
  createdAt: string
  updatedAt: string
}

export async function listAiActionProposals(jobId: string): Promise<AiActionProposal[]> {
  return (await apiClient.get<AiActionProposal[]>(`/v1/ai-resolution-jobs/${jobId}/proposals`)).data
}

export async function approveAiActionProposal(
  jobId: string,
  proposalId: string,
): Promise<AiActionProposal> {
  return (
    await apiClient.post<AiActionProposal>(
      `/v1/ai-resolution-jobs/${jobId}/proposals/${proposalId}/approve`,
    )
  ).data
}

export async function rejectAiActionProposal(
  jobId: string,
  proposalId: string,
): Promise<AiActionProposal> {
  return (
    await apiClient.post<AiActionProposal>(
      `/v1/ai-resolution-jobs/${jobId}/proposals/${proposalId}/reject`,
    )
  ).data
}
