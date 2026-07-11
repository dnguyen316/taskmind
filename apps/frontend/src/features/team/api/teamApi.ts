import { apiClient } from '../../../lib/apiClient'
import type { ProjectMembership, ProjectMembershipRole } from '../../projects/types'
import type {
  AssignTeamMemberProjectPayload,
  ChangeTeamMemberGlobalRolePayload,
  ChangeTeamMemberProjectRolePayload,
  GlobalRoleResponse,
  TeamDirectory,
  TeamMember,
} from '../types'

export async function getTeamDirectory() {
  const response = await apiClient.get<unknown>('/v1/team/directory')
  return adaptTeamDirectory(response.data)
}

export async function assignTeamMemberToProject(
  userId: string,
  projectId: string,
  payload: AssignTeamMemberProjectPayload,
) {
  const response = await apiClient.post<unknown>(
    `/v1/team/members/${userId}/projects/${projectId}`,
    payload,
  )
  return adaptProjectMembership(response.data)
}

export async function updateTeamMemberProjectRole(
  userId: string,
  projectId: string,
  payload: ChangeTeamMemberProjectRolePayload,
) {
  const response = await apiClient.patch<unknown>(
    `/v1/team/members/${userId}/projects/${projectId}/role`,
    payload,
  )
  return adaptProjectMembership(response.data)
}

export async function removeTeamMemberFromProject(userId: string, projectId: string) {
  await apiClient.delete(`/v1/team/members/${userId}/projects/${projectId}`)
}

export async function updateTeamMemberGlobalRole(
  userId: string,
  payload: ChangeTeamMemberGlobalRolePayload,
) {
  const response = await apiClient.patch<unknown>(`/v1/team/members/${userId}/roles`, payload)
  return adaptGlobalRoleResponse(response.data)
}

function adaptTeamDirectory(data: unknown): TeamDirectory {
  if (!isObject(data)) {
    throw new Error('Invalid team directory response.')
  }

  return {
    members: readArray(data.members, adaptTeamMember, 'team directory members'),
    totalMembers: readRequiredNumber(data, 'totalMembers', 'team directory'),
    totalOpenTasks: readRequiredNumber(data, 'totalOpenTasks', 'team directory'),
  }
}

function adaptTeamMember(data: unknown): TeamMember {
  if (!isObject(data)) {
    throw new Error('Invalid team member response.')
  }

  return {
    userId: readRequiredString(data, 'userId', 'team member'),
    displayName: readRequiredString(data, 'displayName', 'team member'),
    email: readRequiredString(data, 'email', 'team member'),
    openTasks: readRequiredNumber(data, 'openTasks', 'team member'),
    globalRole: readOptionalString(data, 'globalRole') ?? readOptionalString(data, 'role'),
  }
}

function adaptProjectMembership(data: unknown): ProjectMembership {
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

function adaptGlobalRoleResponse(data: unknown): GlobalRoleResponse {
  if (!isObject(data)) {
    throw new Error('Invalid global role response.')
  }

  return {
    userId: readRequiredString(data, 'userId', 'global role'),
    role: readRequiredString(data, 'role', 'global role'),
  }
}

function readArray<T>(data: unknown, adapt: (item: unknown) => T, resourceName: string) {
  if (!Array.isArray(data)) {
    throw new Error(`Invalid ${resourceName} response.`)
  }

  return data.map(adapt)
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

function readOptionalString(data: Record<string, unknown>, key: string) {
  const value = data[key]

  if (value === null || value === undefined) {
    return null
  }

  if (typeof value !== 'string') {
    throw new Error(`Invalid response: ${key} must be a string or null.`)
  }

  return value
}

function readRequiredNumber(data: Record<string, unknown>, key: string, resourceName: string) {
  const value = data[key]

  if (typeof value !== 'number' || !Number.isFinite(value)) {
    throw new Error(`Invalid ${resourceName} response: missing ${key}.`)
  }

  return value
}

function isProjectMembershipRole(role: string): role is ProjectMembershipRole {
  return ['OWNER', 'ADMIN', 'MEMBER', 'VIEWER'].includes(role)
}
