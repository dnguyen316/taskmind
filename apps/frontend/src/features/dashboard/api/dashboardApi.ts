import { apiClient } from '../../../lib/apiClient'
import type {
  DashboardActivitySnippet,
  DashboardKpis,
  DashboardResponse,
  DashboardTaskItem,
} from '../types'

export async function getDashboard() {
  const response = await apiClient.get<unknown>('/v1/dashboard')
  return adaptDashboardResponse(response.data)
}

function adaptDashboardResponse(data: unknown): DashboardResponse {
  if (!isObject(data)) throw new Error('Invalid dashboard response.')
  return {
    kpis: adaptKpis(data.kpis),
    myTasks: readArray(data.myTasks, adaptTask, 'dashboard tasks'),
    activity: readArray(data.activity, adaptActivity, 'dashboard activity'),
  }
}
function adaptKpis(data: unknown): DashboardKpis {
  if (!isObject(data)) throw new Error('Invalid dashboard KPIs response.')
  return {
    openTasks: readRequiredNumber(data, 'openTasks', 'dashboard KPIs'),
    completedTasks: readRequiredNumber(data, 'completedTasks', 'dashboard KPIs'),
    eventsIngested: readRequiredNumber(data, 'eventsIngested', 'dashboard KPIs'),
    completionRate: readRequiredNumber(data, 'completionRate', 'dashboard KPIs'),
  }
}
function adaptTask(data: unknown): DashboardTaskItem {
  if (!isObject(data)) throw new Error('Invalid dashboard task response.')
  return {
    taskId: readRequiredString(data, 'taskId', 'dashboard task'),
    projectId: readOptionalString(data, 'projectId', 'dashboard task'),
    title: readRequiredString(data, 'title', 'dashboard task'),
    status: readRequiredString(data, 'status', 'dashboard task'),
    updatedAt: readRequiredString(data, 'updatedAt', 'dashboard task'),
  }
}
function adaptActivity(data: unknown): DashboardActivitySnippet {
  if (!isObject(data)) throw new Error('Invalid dashboard activity response.')
  return {
    date: readRequiredString(data, 'date', 'dashboard activity'),
    tasksCreated: readRequiredNumber(data, 'tasksCreated', 'dashboard activity'),
    tasksCompleted: readRequiredNumber(data, 'tasksCompleted', 'dashboard activity'),
    eventsIngested: readRequiredNumber(data, 'eventsIngested', 'dashboard activity'),
  }
}
function isObject(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null
}
function readArray<T>(data: unknown, adapt: (item: unknown) => T, resourceName: string) {
  if (!Array.isArray(data)) throw new Error(`Invalid ${resourceName} response.`)
  return data.map(adapt)
}
function readRequiredString(data: Record<string, unknown>, key: string, resourceName: string) {
  const value = data[key]
  if (typeof value !== 'string' || value.length === 0)
    throw new Error(`Invalid ${resourceName} response: missing ${key}.`)
  return value
}
function readOptionalString(data: Record<string, unknown>, key: string, resourceName: string) {
  const value = data[key]
  if (value === null || value === undefined) return null
  if (typeof value !== 'string')
    throw new Error(`Invalid ${resourceName} response: invalid ${key}.`)
  return value
}
function readRequiredNumber(data: Record<string, unknown>, key: string, resourceName: string) {
  const value = data[key]
  if (typeof value !== 'number' || !Number.isFinite(value))
    throw new Error(`Invalid ${resourceName} response: missing ${key}.`)
  return value
}
