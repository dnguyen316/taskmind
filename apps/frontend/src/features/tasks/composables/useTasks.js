import { computed, reactive, ref } from 'vue'
import { createTask, listTasks, updateTaskStatus } from '../api/tasksApi'
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

  const visibleTasks = computed(() => tasks.value)

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
