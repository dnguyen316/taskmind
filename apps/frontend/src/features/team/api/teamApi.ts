import { apiClient } from '../../../lib/apiClient'
import type { TeamDirectory, TeamMember } from '../types'

export async function getTeamDirectory() {
  const response = await apiClient.get<unknown>('/v1/team/directory')
  return adaptTeamDirectory(response.data)
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

function readRequiredNumber(data: Record<string, unknown>, key: string, resourceName: string) {
  const value = data[key]

  if (typeof value !== 'number' || !Number.isFinite(value)) {
    throw new Error(`Invalid ${resourceName} response: missing ${key}.`)
  }

  return value
}
