export type AiResolutionJobStatus =
  | 'QUEUED'
  | 'RUNNING'
  | 'WAITING_FOR_APPROVAL'
  | 'SUCCEEDED'
  | 'FAILED'
  | 'CANCELED'
  | 'PAUSED'
export interface AiTaskResolutionJob {
  id: string
  taskId: string
  projectId: string
  templateId: string | null
  githubProjectLinkId: string | null
  status: AiResolutionJobStatus
  currentStep: string | null
  resultSummary: string | null
  errorCode: string | null
  createdAt: string
  updatedAt: string
  completedAt: string | null
}
export interface AiTaskResolutionProposal {
  id: string
  jobId: string
  proposedActionType: string
  payloadPreview: string
  riskLevel: string
  rationale: string | null
  status: 'PENDING' | 'APPROVED' | 'REJECTED' | 'EXECUTED' | 'FAILED'
  errorCode: string | null
  createdAt: string
  updatedAt: string
}
export interface AiTaskResolutionRequest {
  templateId: string | null
  githubProjectLinkId: string | null
  idempotencyKey: string | null
}
