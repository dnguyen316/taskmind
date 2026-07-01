import { apiClient } from '../../../lib/apiClient'
import type {
  AiTaskResolutionJob,
  AiTaskResolutionProposal,
  AiTaskResolutionRequest,
} from '../types'
export async function createAiTaskResolutionJob(taskId: string, payload: AiTaskResolutionRequest) {
  const response = await apiClient.post<AiTaskResolutionJob>(
    `/v1/tasks/${taskId}/ai-resolution-jobs`,
    payload,
  )
  return response.data
}
export async function listAiTaskResolutionJobs(taskId: string) {
  const response = await apiClient.get<AiTaskResolutionJob[]>(
    `/v1/tasks/${taskId}/ai-resolution-jobs`,
  )
  return response.data
}
export async function listAiTaskResolutionProposals(jobId: string) {
  const response = await apiClient.get<AiTaskResolutionProposal[]>(
    `/v1/ai-resolution-jobs/${jobId}/proposals`,
  )
  return response.data
}
export async function approveAiTaskResolutionProposal(jobId: string, proposalId: string) {
  const response = await apiClient.post<AiTaskResolutionProposal>(
    `/v1/ai-resolution-jobs/${jobId}/proposals/${proposalId}/approve`,
  )
  return response.data
}
export async function rejectAiTaskResolutionProposal(jobId: string, proposalId: string) {
  const response = await apiClient.post<AiTaskResolutionProposal>(
    `/v1/ai-resolution-jobs/${jobId}/proposals/${proposalId}/reject`,
  )
  return response.data
}
