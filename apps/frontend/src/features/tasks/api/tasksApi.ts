import { apiClient } from '../../../lib/apiClient'
import type { CreateTaskPayload, Task, TaskStatus, UpdateTaskPayload } from '../types'

export async function listTasks({ userId, status, overdueOnly = false, size = 50 }: { userId: string; status?: TaskStatus; overdueOnly?: boolean; size?: number }) {
  const response = await apiClient.get<Task[]>('/v1/tasks', {
    params: {
      userId,
      status,
      overdueOnly,
      size,
    },
  })

  return response.data
}

export async function createTask(payload: CreateTaskPayload) {
  const response = await apiClient.post<Task>('/v1/tasks', payload)
  return response.data
}

export async function updateTask(taskId: string, payload: UpdateTaskPayload) {
  const response = await apiClient.patch<Task>(`/v1/tasks/${taskId}`, payload)
  return response.data
}

export async function getTaskById(taskId: string, { userId, size = 200 }: { userId: string; size?: number }) {
  const taskList = await listTasks({ userId, size })
  return (Array.isArray(taskList) ? taskList : []).find((candidate) => candidate.id === taskId) ?? null
}

export async function updateTaskStatus(taskId: string, status: TaskStatus) {
  await apiClient.patch(`/v1/tasks/${taskId}/status`, { status })
}
