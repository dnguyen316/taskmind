import type { Task } from '../types'
import { isTaskOverdue } from './taskDates'

export type TaskHealth = 'archived' | 'overdue' | 'stale' | 'blocked' | 'unplanned' | 'healthy'

export function taskHealth(task: Task): TaskHealth {
  if (task.status === 'ARCHIVED') return 'archived'
  if (isTaskOverdue(task)) return 'overdue'
  if (!task.dueAt) return 'unplanned'
  const updatedAt = new Date(task.updatedAt).getTime()
  if (Number.isFinite(updatedAt) && Date.now() - updatedAt > 14 * 24 * 60 * 60 * 1000)
    return 'stale'
  if (task.priority <= 1) return 'blocked'
  return 'healthy'
}

export function taskHealthLabel(health: TaskHealth) {
  return {
    archived: 'Archived',
    overdue: 'Overdue',
    stale: 'Stale',
    blocked: 'At risk',
    unplanned: 'Unplanned',
    healthy: 'Healthy',
  }[health]
}

export function taskHealthColor(health: TaskHealth) {
  return {
    archived: 'default',
    overdue: 'red',
    stale: 'orange',
    blocked: 'volcano',
    unplanned: 'blue',
    healthy: 'green',
  }[health]
}
