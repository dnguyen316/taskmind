import type { Task } from '../types'

export function toTimestamp(value: string | null | undefined): number {
  if (!value) {
    return Number.NaN
  }

  const timestamp = new Date(value).getTime()
  return Number.isFinite(timestamp) ? timestamp : Number.NaN
}

export function isTaskOverdue(task: Task, now = Date.now()): boolean {
  if (task.status === 'DONE' || !task.dueAt) {
    return false
  }

  const dueTimestamp = toTimestamp(task.dueAt)
  return Number.isFinite(dueTimestamp) && dueTimestamp < now
}

export function formatDateTime(value: string | null | undefined): string {
  const timestamp = toTimestamp(value)
  if (!Number.isFinite(timestamp)) {
    return '—'
  }

  return new Date(timestamp).toLocaleString()
}
