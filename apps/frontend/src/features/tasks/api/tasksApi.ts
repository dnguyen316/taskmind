import { apiClient } from '../../../lib/apiClient'
import type { CreateTaskPayload, EnergyLevel, Task, TaskStatus, UpdateTaskPayload } from '../types'

export async function listTasks({
  userId,
  status,
  overdueOnly = false,
  size = 50,
}: {
  userId: string
  status?: TaskStatus
  overdueOnly?: boolean
  size?: number
}) {
  const response = await apiClient.get<unknown>('/v1/tasks', {
    params: {
      userId,
      status,
      overdueOnly,
      size,
    },
  })

  return adaptTaskListResponse(response.data)
}

export async function createTask(payload: CreateTaskPayload) {
  const response = await apiClient.post<unknown>('/v1/tasks', payload)
  return adaptTaskResponse(response.data)
}

export async function updateTask(taskId: string, payload: UpdateTaskPayload) {
  const response = await apiClient.patch<unknown>(`/v1/tasks/${taskId}`, payload)
  return adaptTaskResponse(response.data)
}

export async function getTaskById(taskId: string) {
  const response = await apiClient.get<unknown>(`/v1/tasks/${taskId}`)
  return adaptTaskResponse(response.data)
}

export async function updateTaskStatus(taskId: string, status: TaskStatus) {
  await apiClient.patch(`/v1/tasks/${taskId}/status`, { status })
}

function adaptTaskListResponse(data: unknown): Task[] {
  if (!Array.isArray(data)) {
    throw new Error('Invalid task list response.')
  }

  return data.map(adaptTaskResponse)
}

function adaptTaskResponse(data: unknown): Task {
  if (!isObject(data)) {
    throw new Error('Invalid task response.')
  }

  const status = readRequiredString(data, 'status', 'task')
  const energyLevel = readNullableString(data, 'energyLevel')

  if (!isTaskStatus(status)) {
    throw new Error('Invalid task status response.')
  }

  if (energyLevel !== null && !isEnergyLevel(energyLevel)) {
    throw new Error('Invalid task energy level response.')
  }

  return {
    id: readRequiredString(data, 'id', 'task'),
    version: readNullableNumber(data, 'version'),
    projectId: readNullableString(data, 'projectId') ?? '',
    userId: readRequiredString(data, 'userId', 'task'),
    title: readRequiredString(data, 'title', 'task'),
    description: readNullableString(data, 'description'),
    status,
    priority: readRequiredNumber(data, 'priority', 'task'),
    dueAt: readNullableString(data, 'dueAt'),
    durationMinutes: readNullableNumber(data, 'durationMinutes'),
    energyLevel,
    source: readRequiredString(data, 'source', 'task'),
    confidence: readNullableNumber(data, 'confidence'),
    createdAt: readRequiredString(data, 'createdAt', 'task'),
    updatedAt: readRequiredString(data, 'updatedAt', 'task'),
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

function readRequiredNumber(data: Record<string, unknown>, key: string, resourceName: string) {
  const value = data[key]

  if (typeof value !== 'number' || !Number.isFinite(value)) {
    throw new Error(`Invalid ${resourceName} response: missing ${key}.`)
  }

  return value
}

function readNullableNumber(data: Record<string, unknown>, key: string) {
  const value = data[key]

  if (value === null || value === undefined) {
    return null
  }

  if (typeof value !== 'number' || !Number.isFinite(value)) {
    throw new Error(`Invalid response: ${key} must be a number or null.`)
  }

  return value
}

function isTaskStatus(status: string): status is TaskStatus {
  return ['TODO', 'IN_PROGRESS', 'DONE', 'ARCHIVED'].includes(status)
}

function isEnergyLevel(energyLevel: string): energyLevel is EnergyLevel {
  return ['LOW', 'MEDIUM', 'HIGH'].includes(energyLevel)
}
