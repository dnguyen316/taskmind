import { computed, reactive, ref } from 'vue'
import { useCurrentUserId } from '../../../composables/useCurrentUserId'
import { useProjectsStore } from '../../../stores/projects'
import { createTask, getTaskById, listTasks, updateTask, updateTaskStatus } from '../api/tasksApi'
import type { Project } from '../../projects/types'
import type { CreateTaskPayload, Task, TaskFilters, TaskStatus, UpdateTaskPayload } from '../types'
import { toTimestamp } from '../utils/taskDates'

export function useTasks() {
  const loading = ref(false)
  const saving = ref(false)
  const errorMessage = ref('')
  const pendingStatusTaskIds = ref<string[]>([])
  const tasks = ref<Task[]>([])
  const projectsStore = useProjectsStore()
  const { requireCurrentUserId } = useCurrentUserId()
  const projects = computed<Project[]>(() =>
    projectsStore.projects.filter((project) => !project.archivedAt),
  )
  const activeProjectId = ref('')

  const filters = reactive<TaskFilters>({
    status: undefined,
    overdueOnly: false,
    searchText: '',
    projectId: undefined,
    sortBy: undefined,
  })

  const visibleTasks = computed(() => {
    const filteredTasks = tasks.value.filter((task) => {
      if (filters.projectId && task.projectId !== filters.projectId) {
        return false
      }

      const searchText = filters.searchText.trim().toLowerCase()

      if (!searchText) {
        return true
      }

      return [task.id, task.title, task.description ?? ''].some((value) =>
        value.toLowerCase().includes(searchText),
      )
    })

    const sortBy = filters.sortBy

    if (!sortBy) {
      return filteredTasks
    }

    return [...filteredTasks].sort((taskA, taskB) => compareTasks(taskA, taskB, sortBy))
  })

  async function fetchTasks() {
    loading.value = true
    errorMessage.value = ''

    try {
      tasks.value = await listTasks({
        userId: requireCurrentUserId(),
        status: filters.status,
        overdueOnly: filters.overdueOnly,
      })
    } catch (error: unknown) {
      errorMessage.value = error instanceof Error ? error.message : 'Failed to load tasks.'
    } finally {
      loading.value = false
    }
  }

  async function fetchProjects() {
    errorMessage.value = ''

    try {
      await projectsStore.fetchProjects()

      if (!activeProjectId.value && projects.value.length > 0) {
        const currentProject =
          projects.value.find((project) => !project.archivedAt) ?? projects.value[0]
        activeProjectId.value = currentProject?.id ?? ''
      }
    } catch (error: unknown) {
      errorMessage.value = error instanceof Error ? error.message : 'Failed to load projects.'
    }
  }

  async function submitTask(formValues: Omit<CreateTaskPayload, 'userId' | 'source'>) {
    saving.value = true
    errorMessage.value = ''

    try {
      if (!formValues.projectId) {
        throw new Error('Please select a valid project before creating a task.')
      }

      await createTask({
        userId: requireCurrentUserId(),
        source: 'MANUAL',
        ...formValues,
      })

      await fetchTasks()
    } catch (error: unknown) {
      errorMessage.value = error instanceof Error ? error.message : 'Failed to create task.'
      throw error
    } finally {
      saving.value = false
    }
  }

  async function changeStatus(taskId: string, status: TaskStatus) {
    errorMessage.value = ''
    pendingStatusTaskIds.value = [...pendingStatusTaskIds.value, taskId]

    try {
      await updateTaskStatus(taskId, status)
      await fetchTasks()
    } catch (error: unknown) {
      errorMessage.value = error instanceof Error ? error.message : 'Failed to update status.'
    } finally {
      pendingStatusTaskIds.value = pendingStatusTaskIds.value.filter(
        (pendingTaskId) => pendingTaskId !== taskId,
      )
    }
  }

  async function fetchTaskById(taskId: string) {
    loading.value = true
    errorMessage.value = ''

    try {
      return await getTaskById(taskId)
    } catch (error: unknown) {
      errorMessage.value = error instanceof Error ? error.message : 'Failed to load task.'
      return null
    } finally {
      loading.value = false
    }
  }

  async function updateTaskDetails(taskId: string, payload: UpdateTaskPayload) {
    saving.value = true
    errorMessage.value = ''

    try {
      const updatedTask = await updateTask(taskId, payload)

      tasks.value = tasks.value.map((task) => (task.id === taskId ? updatedTask : task))
      return updatedTask
    } catch (error: unknown) {
      errorMessage.value = error instanceof Error ? error.message : 'Failed to update task details.'
      throw error
    } finally {
      saving.value = false
    }
  }

  return {
    loading,
    saving,
    errorMessage,
    pendingStatusTaskIds,
    filters,
    visibleTasks,
    projects,
    activeProjectId,
    fetchTasks,
    fetchProjects,
    submitTask,
    changeStatus,
    fetchTaskById,
    updateTaskDetails,
  }
}

function compareTasks(taskA: Task, taskB: Task, sortBy: NonNullable<TaskFilters['sortBy']>) {
  if (sortBy === 'priority') {
    return taskB.priority - taskA.priority
  }

  if (sortBy === 'dueAt') {
    return compareNullableDates(taskA.dueAt, taskB.dueAt)
  }

  return compareNullableDates(
    taskA.updatedAt ?? taskA.createdAt,
    taskB.updatedAt ?? taskB.createdAt,
  )
}

function compareNullableDates(dateA: string | null, dateB: string | null) {
  const timestampA = toTimestamp(dateA)
  const timestampB = toTimestamp(dateB)
  const safeTimestampA = Number.isFinite(timestampA) ? timestampA : 0
  const safeTimestampB = Number.isFinite(timestampB) ? timestampB : 0

  return safeTimestampB - safeTimestampA
}
