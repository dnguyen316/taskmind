import { apiClient } from '../../../lib/apiClient'
import type {
  AiApprovalPolicy,
  AiWorkflowTemplate,
  AiWorkflowTemplatePayload,
  AiWorkflowType,
} from '../types'

export async function listAiWorkflowTemplates(projectId: string) {
  const response = await apiClient.get<unknown[]>(`/v1/projects/${projectId}/ai-workflow-templates`)
  return response.data.map(adaptTemplate)
}
export async function createAiWorkflowTemplate(
  projectId: string,
  payload: AiWorkflowTemplatePayload,
) {
  const response = await apiClient.post<unknown>(
    `/v1/projects/${projectId}/ai-workflow-templates`,
    payload,
  )
  return adaptTemplate(response.data)
}
export async function updateAiWorkflowTemplate(
  templateId: string,
  payload: AiWorkflowTemplatePayload,
) {
  const response = await apiClient.put<unknown>(`/v1/ai-workflow-templates/${templateId}`, payload)
  return adaptTemplate(response.data)
}
export async function archiveAiWorkflowTemplate(templateId: string) {
  await apiClient.delete(`/v1/ai-workflow-templates/${templateId}`)
}

function adaptTemplate(data: unknown): AiWorkflowTemplate {
  const v = record(data)
  return {
    id: string(v, 'id'),
    projectId: string(v, 'projectId'),
    version: numberOrNull(v.version),
    archivedAt: nullable(v, 'archivedAt'),
    createdAt: string(v, 'createdAt'),
    updatedAt: string(v, 'updatedAt'),
    name: string(v, 'name'),
    description: nullable(v, 'description'),
    workflowType: string(v, 'workflowType') as AiWorkflowType,
    templateBody: string(v, 'templateBody'),
    allowedTools: nullable(v, 'allowedTools') ?? '[]',
    approvalPolicy: string(v, 'approvalPolicy') as AiApprovalPolicy,
    autoApproveReadOnly: Boolean(v.autoApproveReadOnly),
    requireApprovalForComments: Boolean(v.requireApprovalForComments),
    requireApprovalForBranch: Boolean(v.requireApprovalForBranch),
    requireApprovalForPullRequest: Boolean(v.requireApprovalForPullRequest),
    requireApprovalForTaskMutation: Boolean(v.requireApprovalForTaskMutation),
    defaultModelPolicy: nullable(v, 'defaultModelPolicy'),
  }
}
function record(value: unknown): Record<string, unknown> {
  if (typeof value !== 'object' || value === null)
    throw new Error('Invalid AI workflow template response.')
  return value as Record<string, unknown>
}
function string(v: Record<string, unknown>, k: string) {
  if (typeof v[k] !== 'string') throw new Error(`Invalid template response: missing ${k}.`)
  return v[k] as string
}
function nullable(v: Record<string, unknown>, k: string) {
  return typeof v[k] === 'string' ? (v[k] as string) : null
}
function numberOrNull(v: unknown) {
  return typeof v === 'number' ? v : null
}
