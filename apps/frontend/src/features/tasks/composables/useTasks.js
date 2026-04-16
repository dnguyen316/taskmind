import { computed, reactive, ref } from 'vue'
import { createTask, getTaskById, listTasks, updateTask, updateTaskStatus } from '../api/tasksApi'
import { DEFAULT_USER_ID } from '../constants/taskConstants'

export function useTasks() {
  const loading = ref(false)
  const saving = ref(false)
  const errorMessage = ref('')
  const tasks = ref([])

  const filters = reactive({
    status: undefined,
    overdueOnly: false,
  })

  const visibleTasks = computed(() => [...tasks.value].sort(compareByRecency))

  async function fetchTasks() {
    loading.value = true
    errorMessage.value = ''

    try {
      const response = await listTasks({
        userId: DEFAULT_USER_ID,
        status: filters.status,
        overdueOnly: filters.overdueOnly,
      })

      tasks.value = Array.isArray(response) ? response : []
    } catch (error) {
      errorMessage.value = error instanceof Error ? error.message : 'Failed to load tasks.'
    } finally {
      loading.value = false
    }
  }

  async function submitTask(formValues) {
    saving.value = true
    errorMessage.value = ''

    try {
      await createTask({
        userId: DEFAULT_USER_ID,
        source: 'MANUAL',
        ...formValues,
      })

      await fetchTasks()
    } catch (error) {
      errorMessage.value = error instanceof Error ? error.message : 'Failed to create task.'
      throw error
    } finally {
      saving.value = false
    }
  }

  async function changeStatus(taskId, status) {
    errorMessage.value = ''

    try {
      await updateTaskStatus(taskId, status)
      await fetchTasks()
    } catch (error) {
      errorMessage.value = error instanceof Error ? error.message : 'Failed to update status.'
    }
  }

  async function fetchTaskById(taskId) {
    loading.value = true
    errorMessage.value = ''

    try {
      return await getTaskById(taskId, { userId: DEFAULT_USER_ID })
    } catch (error) {
      errorMessage.value = error instanceof Error ? error.message : 'Failed to load task.'
      return null
    } finally {
      loading.value = false
    }
  }

  async function updateTaskDetails(taskId, payload) {
    saving.value = true
    errorMessage.value = ''

    try {
      const updatedTask = await updateTask(taskId, payload)

      tasks.value = tasks.value.map((task) => (task.id === taskId ? updatedTask : task))
      return updatedTask
    } catch (error) {
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
    filters,
    visibleTasks,
    fetchTasks,
    submitTask,
    changeStatus,
    fetchTaskById,
    updateTaskDetails,
  }
}

function compareByRecency(taskA, taskB) {
  const taskATime = getRecencyTimestamp(taskA)
  const taskBTime = getRecencyTimestamp(taskB)

  return taskBTime - taskATime
}

function getRecencyTimestamp(task) {
  const primaryDate = toTimestamp(task?.updatedAt)
  const fallbackDate = toTimestamp(task?.createdAt)

  if (Number.isFinite(primaryDate)) {
    return primaryDate
  }

  if (Number.isFinite(fallbackDate)) {
    return fallbackDate
  }

  return 0
}

function toTimestamp(value) {
  if (!value) {
    return Number.NaN
  }

  const timestamp = new Date(value).getTime()
  return Number.isFinite(timestamp) ? timestamp : Number.NaN
}
