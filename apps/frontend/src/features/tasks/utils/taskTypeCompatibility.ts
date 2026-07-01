import type { TaskLevel, TaskTypeDefinition } from '../types'

export function isTaskTypeAllowedForLevel(
  taskType: TaskTypeDefinition | null | undefined,
  level: TaskLevel | null | undefined,
): boolean {
  if (!taskType || !level) {
    return false
  }

  return taskType.allowedTaskLevels.includes(level)
}

export function findTaskTypeDefinition(
  taskTypes: TaskTypeDefinition[],
  type: string | null | undefined,
): TaskTypeDefinition | null {
  const normalizedType = type?.trim().toUpperCase()
  if (!normalizedType) {
    return null
  }

  return taskTypes.find((taskType) => taskType.key.trim().toUpperCase() === normalizedType) ?? null
}
