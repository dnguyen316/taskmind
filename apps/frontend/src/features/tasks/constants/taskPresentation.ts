import type { TaskStatus } from '../types'

export interface TaskStatusMetadata {
  label: string
  color: string
}

export const TASK_STATUS_PRESENTATION: Record<TaskStatus, TaskStatusMetadata> = {
  TODO: { label: 'To do', color: 'default' },
  IN_PROGRESS: { label: 'In progress', color: 'blue' },
  DONE: { label: 'Done', color: 'green' },
  ARCHIVED: { label: 'Archived', color: 'purple' },
}

export const TASK_STATUS_SELECT_OPTIONS: Array<{ label: string; value: TaskStatus }> =
  Object.entries(TASK_STATUS_PRESENTATION).map(([value, metadata]) => ({
    label: metadata.label,
    value: value as TaskStatus,
  }))

export function getTaskStatusPresentation(status: TaskStatus): TaskStatusMetadata {
  return TASK_STATUS_PRESENTATION[status]
}
