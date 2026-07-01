import { apiClient } from '../../../lib/apiClient'
import type { TaskLevel, TaskTypeDefinition } from '../types'

const TASK_LEVELS: readonly TaskLevel[] = ['EPIC', 'STORY', 'TASK', 'SUBTASK']

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
  const key = readString(record, 'key')
  const defaultTaskLevel = readTaskLevel(record, 'defaultTaskLevel') ?? inferDefaultTaskLevel(key)
  const allowedTaskLevels =
    readTaskLevelList(record, 'allowedTaskLevels') ?? inferAllowedTaskLevels(key, defaultTaskLevel)

  return {
    id: readString(record, 'id'),
    version: readNullableNumber(record, 'version'),
    projectId: readNullableString(record, 'projectId'),
    key,
    name: readString(record, 'name'),
    color: readNullableString(record, 'color'),
    icon: readNullableString(record, 'icon'),
    defaultTaskLevel,
    allowedTaskLevels,
    container: readBoolean(record, 'container', inferContainer(key, defaultTaskLevel)),
    allowChildren: readBoolean(record, 'allowChildren', inferAllowChildren(key, defaultTaskLevel)),
    system: readBoolean(record, 'system'),
    active: readBoolean(record, 'active'),
    sortOrder: readNullableNumber(record, 'sortOrder'),
    createdAt: readString(record, 'createdAt'),
    updatedAt: readString(record, 'updatedAt'),
  }
}

function readString(data: Record<string, unknown>, key: string) {
  const value = data[key]
  if (typeof value !== 'string' || value.length === 0)
    throw new Error(`Invalid task type response: missing ${key}.`)
  return value
}

function readNullableString(data: Record<string, unknown>, key: string) {
  const value = data[key]
  if (value === null || value === undefined) return null
  if (typeof value !== 'string')
    throw new Error(`Invalid task type response: ${key} must be a string or null.`)
  return value
}

function readNullableNumber(data: Record<string, unknown>, key: string) {
  const value = data[key]
  if (value === null || value === undefined) return null
  if (typeof value !== 'number' || !Number.isFinite(value))
    throw new Error(`Invalid task type response: ${key} must be a number or null.`)
  return value
}

function readBoolean(data: Record<string, unknown>, key: string, fallback?: boolean) {
  const value = data[key]
  if (value === undefined && fallback !== undefined) return fallback
  if (typeof value !== 'boolean') throw new Error(`Invalid task type response: missing ${key}.`)
  return value
}

function readTaskLevel(data: Record<string, unknown>, key: string): TaskLevel | null {
  const value = data[key]
  if (value === undefined || value === null) return null
  if (!isTaskLevel(value))
    throw new Error(`Invalid task type response: ${key} must be a task level.`)
  return value
}

function readTaskLevelList(data: Record<string, unknown>, key: string): TaskLevel[] | null {
  const value = data[key]
  if (value === undefined || value === null) return null
  if (!Array.isArray(value) || value.length === 0 || !value.every(isTaskLevel)) {
    throw new Error(`Invalid task type response: ${key} must be a non-empty task level array.`)
  }

  return [...new Set(value)]
}

function isTaskLevel(value: unknown): value is TaskLevel {
  return typeof value === 'string' && TASK_LEVELS.includes(value as TaskLevel)
}

function inferDefaultTaskLevel(key: string): TaskLevel {
  const normalizedKey = key.trim().toUpperCase()
  return isTaskLevel(normalizedKey) ? normalizedKey : 'TASK'
}

function inferAllowedTaskLevels(key: string, defaultTaskLevel: TaskLevel): TaskLevel[] {
  const normalizedKey = key.trim().toUpperCase()
  if (normalizedKey === 'MILESTONE') return ['EPIC', 'STORY', 'TASK']
  return [defaultTaskLevel]
}

function inferContainer(key: string, defaultTaskLevel: TaskLevel): boolean {
  const normalizedKey = key.trim().toUpperCase()
  return ['EPIC', 'STORY', 'MILESTONE'].includes(normalizedKey) || defaultTaskLevel !== 'SUBTASK'
}

function inferAllowChildren(key: string, defaultTaskLevel: TaskLevel): boolean {
  const normalizedKey = key.trim().toUpperCase()
  return normalizedKey !== 'SUBTASK' && defaultTaskLevel !== 'SUBTASK'
}
