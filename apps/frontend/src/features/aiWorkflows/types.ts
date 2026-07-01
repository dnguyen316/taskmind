export type AiWorkflowType = 'TASK_RESOLUTION' | 'BUG_TRIAGE' | 'PR_REVIEW'
export type AiApprovalPolicy = 'AUTO' | 'MANUAL' | 'ADMIN_ONLY'
export type AiToolId =
  | 'github.read_issues'
  | 'github.read_code'
  | 'github.create_branch'
  | 'github.create_pr'
  | 'github.comment'
  | 'task.update'

export interface AiWorkflowTemplatePayload {
  name: string
  description: string | null
  workflowType: AiWorkflowType
  templateBody: string
  allowedTools: string
  approvalPolicy: AiApprovalPolicy
  autoApproveReadOnly: boolean
  requireApprovalForComments: boolean
  requireApprovalForBranch: boolean
  requireApprovalForPullRequest: boolean
  requireApprovalForTaskMutation: boolean
  defaultModelPolicy: string | null
}

export interface AiWorkflowTemplate extends AiWorkflowTemplatePayload {
  id: string
  projectId: string
  version: number | null
  archivedAt: string | null
  createdAt: string
  updatedAt: string
}

export const AI_TOOL_OPTIONS: Array<{ label: string; value: AiToolId }> = [
  { label: 'Read GitHub issues', value: 'github.read_issues' },
  { label: 'Read repository code', value: 'github.read_code' },
  { label: 'Create branch', value: 'github.create_branch' },
  { label: 'Create pull request', value: 'github.create_pr' },
  { label: 'Comment on GitHub', value: 'github.comment' },
  { label: 'Update task fields', value: 'task.update' },
]
