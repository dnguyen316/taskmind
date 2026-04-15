import { computed, reactive, ref } from 'vue'
import { createTask, listTasks, updateTaskStatus } from '../api/tasksApi'
import {
  DEFAULT_CREATE_TASK_FORM,
  DEFAULT_USER_ID,
} from '../constants/taskConstants'

function emptyCreateForm() {
  return {
    ...DEFAULT_CREATE_TASK_FORM,
  }
}

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

  const createForm = reactive(emptyCreateForm())

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

  async function submitTask() {
    if (!createForm.title.trim()) {
      errorMessage.value = 'Task title is required.'
      return
    }

    saving.value = true
    errorMessage.value = ''

    try {
      await createTask({
        userId: DEFAULT_USER_ID,
        title: createForm.title.trim(),
        description: createForm.description.trim() || null,
        status: createForm.status,
        priority: Number(createForm.priority),
        dueAt: toApiDateTimeLocal(createForm.dueAt),
        durationMinutes: Number(createForm.durationMinutes),
        energyLevel: 'MEDIUM',
        source: 'MANUAL',
        confidence: null,
      })

      Object.assign(createForm, emptyCreateForm())
      await fetchTasks()
    } catch (error) {
      errorMessage.value = error instanceof Error ? error.message : 'Unable to create task'
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
    tasks,
    loading,
    saving,
    errorMessage,
    filters,
    createForm,
    visibleTasks,
    fetchTasks,
    submitTask,
    changeStatus,
  }
}
