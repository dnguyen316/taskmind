import type { RouteLocationRaw } from 'vue-router'
import type { Task } from '../types'

export function taskDetailRoute(task: Pick<Task, 'id'>, projectId?: string): RouteLocationRaw {
  if (projectId) {
    return { name: 'project-task-detail', params: { projectId, taskId: task.id } }
  }

  return { name: 'task-detail', params: { id: task.id } }
}
