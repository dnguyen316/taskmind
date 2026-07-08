import { apiClient } from '../../../lib/apiClient'
import type {
  AddProjectMemberPayload,
  CreateProjectPayload,
  Project,
  ProjectMembership,
  ProjectHealth,
  ProjectHealthAssigneeWorkload,
  ProjectMembershipRole,
  UpdateProjectPayload,
} from '../types'

interface FetchProjectsOptions {
  includeArchived?: boolean
}

export async function listProjects({ includeArchived = false }: FetchProjectsOptions = {}) {
  const response = await apiClient.get<unknown>('/v1/projects', {
    params: { includeArchived },
  })

  return adaptProjectListResponse(response.data)
}

export async function getProject(projectId: string) {
  const response = await apiClient.get<unknown>(`/v1/projects/${projectId}`)
  return adaptProjectResponse(response.data)
}

export async function getProjectHealth(projectId: string) {
  const response = await apiClient.get<unknown>(`/v1/projects/${projectId}/health`)
  return adaptProjectHealthResponse(response.data)
}

export async function createProject(payload: CreateProjectPayload) {
  const response = await apiClient.post<unknown>('/v1/projects', payload)
  return adaptProjectResponse(response.data)
}

export async function updateProject(projectId: string, payload: UpdateProjectPayload) {
  const response = await apiClient.patch<unknown>(`/v1/projects/${projectId}`, payload)
  return adaptProjectResponse(response.data)
}

export async function archiveProject(projectId: string) {
  const response = await apiClient.patch<unknown>(`/v1/projects/${projectId}/archive`)
  return adaptProjectResponse(response.data)
}

export async function listProjectMembers(projectId: string) {
  const response = await apiClient.get<unknown>(`/v1/projects/${projectId}/members`)
  return adaptProjectMembershipListResponse(response.data)
}

export async function addProjectMember(projectId: string, payload: AddProjectMemberPayload) {
  const response = await apiClient.post<unknown>(`/v1/projects/${projectId}/members`, payload)
  return adaptProjectMembershipResponse(response.data)
}

export async function removeProjectMember(projectId: string, userId: string) {
  await apiClient.delete(`/v1/projects/${projectId}/members/${userId}`)
}

function adaptProjectListResponse(data: unknown): Project[] {
  if (!Array.isArray(data)) {
    throw new Error('Invalid project list response.')
  }

  return data.map(adaptProjectResponse)
}

function adaptProjectResponse(data: unknown): Project {
  if (!isObject(data)) {
    throw new Error('Invalid project response.')
  }

  const id = readRequiredString(data, 'id', 'project')
  const name = readRequiredString(data, 'name', 'project')
  const key = readRequiredString(data, 'key', 'project')
  const ownerUserId = readRequiredString(data, 'ownerUserId', 'project')
  const createdAt = readRequiredString(data, 'createdAt', 'project')
  const updatedAt = readRequiredString(data, 'updatedAt', 'project')

  return {
    id,
    name,
    key,
    description: readNullableString(data, 'description'),
    ownerUserId,
    archivedAt: readNullableString(data, 'archivedAt'),
    createdAt,
    updatedAt,
  }
}

function adaptProjectHealthResponse(data: unknown): ProjectHealth {
  if (!isObject(data)) {
    throw new Error('Invalid project health response.')
  }

  return {
    projectId: readRequiredString(data, 'projectId', 'project health'),
    totalTaskCount: readRequiredNumber(data, 'totalTaskCount'),
    completedTaskCount: readRequiredNumber(data, 'completedTaskCount'),
    completionPercentage: readRequiredNumber(data, 'completionPercentage'),
    overdueTaskCount: readRequiredNumber(data, 'overdueTaskCount'),
    blockedTaskCount: readRequiredNumber(data, 'blockedTaskCount'),
    unassignedTaskCount: readRequiredNumber(data, 'unassignedTaskCount'),
    staleTaskCount: readRequiredNumber(data, 'staleTaskCount'),
    upcomingDeadlineRiskCount: readRequiredNumber(data, 'upcomingDeadlineRiskCount'),
    workloadByAssignee: adaptWorkloadList(data.workloadByAssignee),
    narrative: readRequiredString(data, 'narrative', 'project health'),
    calculatedAt: readRequiredString(data, 'calculatedAt', 'project health'),
  }
}

function adaptWorkloadList(data: unknown): ProjectHealthAssigneeWorkload[] {
  if (!Array.isArray(data)) {
    throw new Error('Invalid project health workload response.')
  }

  return data.map((item) => {
    if (!isObject(item)) {
      throw new Error('Invalid project health workload response.')
    }

    return {
      assigneeId: readRequiredString(item, 'assigneeId', 'project health workload'),
      activeTaskCount: readRequiredNumber(item, 'activeTaskCount'),
    }
  })
}

function adaptProjectMembershipListResponse(data: unknown): ProjectMembership[] {
  if (!Array.isArray(data)) {
    throw new Error('Invalid project membership list response.')
  }

  return data.map(adaptProjectMembershipResponse)
}

function adaptProjectMembershipResponse(data: unknown): ProjectMembership {
  if (!isObject(data)) {
    throw new Error('Invalid project membership response.')
  }

  const role = readRequiredString(data, 'role', 'project membership')

  if (!isProjectMembershipRole(role)) {
    throw new Error('Invalid project membership role response.')
  }

  return {
    projectId: readRequiredString(data, 'projectId', 'project membership'),
    userId: readRequiredString(data, 'userId', 'project membership'),
    role,
  }
}

function isObject(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null
}

function readRequiredString(data: Record<string, unknown>, key: string, resourceName: string) {
  const value = data[key]

  if (typeof value !== 'string' || value.length === 0) {
    throw new Error(`Invalid ${resourceName} response: missing ${key}.`)
  }

  return value
}

function readRequiredNumber(data: Record<string, unknown>, key: string) {
  const value = data[key]

  if (typeof value !== 'number' || !Number.isFinite(value)) {
    throw new Error(`Invalid response: ${key} must be a number.`)
  }

  return value
}

function readNullableString(data: Record<string, unknown>, key: string) {
  const value = data[key]

  if (value === null || value === undefined) {
    return null
  }

  if (typeof value !== 'string') {
    throw new Error(`Invalid response: ${key} must be a string or null.`)
  }

  return value
}

function isProjectMembershipRole(role: string): role is ProjectMembershipRole {
  return ['OWNER', 'ADMIN', 'MEMBER', 'VIEWER'].includes(role)
}
