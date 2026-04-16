import type { CreateTaskPayload, Task, TaskStatus, UpdateTaskPayload } from '../types'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080'

async function request<TResponse>(path: string, options: RequestInit = {}): Promise<TResponse> {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    headers: { 'Content-Type': 'application/json', ...(options.headers ?? {}) },
    ...options,
  })

  if (!response.ok) {
    throw new Error(`${options.method ?? 'GET'} ${path} failed (${response.status})`)
  }

  if (response.status === 204) {
    return null as TResponse
  }

  return response.json() as Promise<TResponse>
}

export function listTasks({ userId, status, overdueOnly = false, size = 50 }: { userId: string; status?: TaskStatus; overdueOnly?: boolean; size?: number }) {
  const query = new URLSearchParams({
    userId,
    overdueOnly: String(overdueOnly),
    size: String(size),
  })

  if (status) {
    query.set('status', status)
  }

  return request<Task[]>(`/v1/tasks?${query.toString()}`)
}

export function createTask(payload: CreateTaskPayload) {
  return request<Task>('/v1/tasks', {
    method: 'POST',
    body: JSON.stringify(payload),
  })
}

export function updateTask(taskId: string, payload: UpdateTaskPayload) {
  return request<Task>(`/v1/tasks/${taskId}`, {
    method: 'PATCH',
    body: JSON.stringify(payload),
  })
}

export async function getTaskById(taskId: string, { userId, size = 200 }: { userId: string; size?: number }) {
  const taskList = await listTasks({ userId, size })
  return (Array.isArray(taskList) ? taskList : []).find((candidate) => candidate.id === taskId) ?? null
}

export function updateTaskStatus(taskId: string, status: TaskStatus) {
  return request<null>(`/v1/tasks/${taskId}/status`, {
    method: 'PATCH',
    body: JSON.stringify({ status }),
  })
}
