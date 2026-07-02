import { apiClient } from '../../../lib/apiClient'
import type {
  CreateSpecDraftPayload,
  MaterializeSpecDraftResponse,
  SpecBreakdownDraft,
  SpecCandidateTree,
} from '../types'

export async function createSpecBreakdownDraft(payload: CreateSpecDraftPayload) {
  const response = await apiClient.post<SpecBreakdownDraft>('/v1/spec-breakdown/drafts', payload)
  return response.data
}

export async function reviewSpecBreakdownDraft(
  draftId: string,
  accepted: boolean,
  candidateTree: SpecCandidateTree,
) {
  const response = await apiClient.post<SpecBreakdownDraft>(
    `/v1/spec-breakdown/drafts/${draftId}/review`,
    {
      accepted,
      candidateTree: JSON.stringify(candidateTree),
    },
  )
  return response.data
}

export async function materializeSpecBreakdownDraft(draftId: string) {
  const response = await apiClient.post<MaterializeSpecDraftResponse>(
    `/v1/spec-breakdown/drafts/${draftId}/materialize`,
  )
  return response.data
}
