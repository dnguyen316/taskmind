import { apiClient } from '../../../lib/apiClient'
import type { TaskTypeDefinition } from '../types'

export async function listTaskTypes(projectId?: string | null) {
  const response = await apiClient.get<unknown>('/v1/task-types', {
    params: projectId ? { projectId } : undefined,
  })

  return adaptTaskTypeListResponse(response.data)
}

function adaptTaskTypeListResponse(data: unknown): TaskTypeDefinition[] {
  if (!Array.isArray(data)) {
    throw new Error('Invalid task type list response.')
  }

  return data.map(adaptTaskTypeResponse)
}

function adaptTaskTypeResponse(data: unknown): TaskTypeDefinition {
  if (typeof data !== 'object' || data === null) {
    throw new Error('Invalid task type response.')
  }

  const record = data as Record<string, unknown>
  return {
    id: readString(record, 'id'),
    version: readNullableNumber(record, 'version'),
    projectId: readNullableString(record, 'projectId'),
    key: readString(record, 'key'),
    name: readString(record, 'name'),
    color: readNullableString(record, 'color'),
    icon: readNullableString(record, 'icon'),
    system: readBoolean(record, 'system'),
    active: readBoolean(record, 'active'),
    sortOrder: readNullableNumber(record, 'sortOrder'),
    createdAt: readString(record, 'createdAt'),
    updatedAt: readString(record, 'updatedAt'),
  }
}

function readString(data: Record<string, unknown>, key: string) {
  const value = data[key]
  if (typeof value !== 'string' || value.length === 0) throw new Error(`Invalid task type response: missing ${key}.`)
  return value
}

function readNullableString(data: Record<string, unknown>, key: string) {
  const value = data[key]
  if (value === null || value === undefined) return null
  if (typeof value !== 'string') throw new Error(`Invalid task type response: ${key} must be a string or null.`)
  return value
}

function readNullableNumber(data: Record<string, unknown>, key: string) {
  const value = data[key]
  if (value === null || value === undefined) return null
  if (typeof value !== 'number' || !Number.isFinite(value)) throw new Error(`Invalid task type response: ${key} must be a number or null.`)
  return value
}

function readBoolean(data: Record<string, unknown>, key: string) {
  const value = data[key]
  if (typeof value !== 'boolean') throw new Error(`Invalid task type response: missing ${key}.`)
  return value
}
