import { apiClient } from '../../../lib/apiClient'
import type {
  CreateTaskPayload,
  EnergyLevel,
  Task,
  TaskLevel,
  TaskStatus,
  UpdateTaskPayload,
  SavedTaskView,
  TaskFilters,
} from '../types'

export async function listTasks({
  userId,
  status,
  overdueOnly = false,
  filters,
  size = 50,
}: {
  userId: string
  status?: TaskStatus
  overdueOnly?: boolean
  filters?: Partial<TaskFilters>
  size?: number
}) {
  const response = await apiClient.get<unknown>('/v1/tasks', {
    params: {
      userId,
      status,
      overdueOnly,
      dueToday: filters?.dueToday,
      blocked: filters?.blocked,
      unassigned: filters?.unassigned,
      noDueDate: filters?.noDueDate,
      stale: filters?.stale,
      archived: filters?.archived,
      priority: filters?.priority,
      projectId: filters?.projectId,
      assigneeId: filters?.assigneeId,
      sort: filters?.sortBy,
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
  const response = await apiClient.patch<unknown>(`/v1/tasks/${taskId}/status`, { status })
  return adaptTaskResponse(response.data)
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
  const taskLevel = readNullableString(data, 'taskLevel')
  const taskType = readNullableString(data, 'taskType')

  if (!isTaskStatus(status)) {
    throw new Error('Invalid task status response.')
  }

  if (energyLevel !== null && !isEnergyLevel(energyLevel)) {
    throw new Error('Invalid task energy level response.')
  }

  if (taskLevel !== null && !isTaskLevel(taskLevel)) {
    throw new Error('Invalid task level response.')
  }

  return {
    id: readRequiredString(data, 'id', 'task'),
    version: readNullableNumber(data, 'version'),
    projectId: readNullableString(data, 'projectId') ?? '',
    userId: readRequiredString(data, 'userId', 'task'),
    taskKey: readNullableString(data, 'taskKey'),
    assigneeId: readNullableString(data, 'assigneeId'),
    parentTaskId: readNullableString(data, 'parentTaskId'),
    taskLevel,
    taskType,
    storyPoints: readNullableNumber(data, 'storyPoints'),
    releaseVersion: readNullableString(data, 'releaseVersion'),
    deletedAt: readNullableString(data, 'deletedAt'),
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

function isTaskLevel(taskLevel: string): taskLevel is TaskLevel {
  return ['EPIC', 'STORY', 'TASK', 'SUBTASK'].includes(taskLevel)
}

export async function listSavedTaskViews() {
  const response = await apiClient.get<unknown>('/v1/task-saved-views')
  if (!Array.isArray(response.data)) throw new Error('Invalid saved views response.')
  return response.data.map(adaptSavedTaskView)
}

export async function createSavedTaskView(name: string, filters: Partial<TaskFilters>) {
  const response = await apiClient.post<unknown>('/v1/task-saved-views', { name, filters })
  return adaptSavedTaskView(response.data)
}

function adaptSavedTaskView(data: unknown): SavedTaskView {
  if (!isObject(data)) throw new Error('Invalid saved view response.')
  const filters = isObject(data.filters) ? (data.filters as Partial<TaskFilters>) : {}
  return {
    id: readRequiredString(data, 'id', 'saved view'),
    version: readNullableNumber(data, 'version'),
    userId: readRequiredString(data, 'userId', 'saved view'),
    name: readRequiredString(data, 'name', 'saved view'),
    filters,
    builtIn: Boolean(data.builtIn),
    createdAt: readRequiredString(data, 'createdAt', 'saved view'),
    updatedAt: readRequiredString(data, 'updatedAt', 'saved view'),
  }
}
