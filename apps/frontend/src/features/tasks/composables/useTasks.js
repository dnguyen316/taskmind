import { computed, reactive, ref } from 'vue'
import { createTask, listTasks, updateTaskStatus } from '../api/tasksApi'
import { DEFAULT_USER_ID } from '../constants/taskConstants'

function toApiDateTimeLocal(value) {
  if (!value) {
    return null
  }
  return `${value}:00`
}

export function useTasks() {
  const tasks = ref([])
  const loading = ref(false)
  const saving = ref(false)
  const errorMessage = ref('')

  const filters = reactive({
    status: '',
    overdueOnly: false,
  })

  const visibleTasks = computed(() => {
    if (!filters.status) {
      return tasks.value
    }
    return tasks.value.filter((task) => task.status === filters.status)
  })

  async function fetchTasks() {
    loading.value = true
    errorMessage.value = ''

    try {
      tasks.value = await listTasks({
        userId: DEFAULT_USER_ID,
        status: filters.status,
        overdueOnly: filters.overdueOnly,
      })
    } catch (error) {
      errorMessage.value = error instanceof Error ? error.message : 'Unable to fetch tasks'
    } finally {
      loading.value = false
    }
  }

  async function submitTask(values) {
    saving.value = true
    errorMessage.value = ''

    try {
      await createTask({
        userId: DEFAULT_USER_ID,
        title: values.title.trim(),
        description: values.description.trim() || null,
        status: values.status,
        priority: Number(values.priority),
        dueAt: toApiDateTimeLocal(values.dueAt),
        durationMinutes: Number(values.durationMinutes),
        energyLevel: 'MEDIUM',
        source: 'MANUAL',
        confidence: null,
      })

      await fetchTasks()
      return { ok: true }
    } catch (error) {
      errorMessage.value = error instanceof Error ? error.message : 'Unable to create task'
      return { ok: false, message: errorMessage.value }
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
      errorMessage.value = error instanceof Error ? error.message : 'Unable to update task status'
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
  }
}
