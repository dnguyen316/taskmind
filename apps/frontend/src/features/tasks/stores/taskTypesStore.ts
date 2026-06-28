import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { listTaskTypes } from '../api/taskTypesApi'
import type { TaskTypeDefinition } from '../types'

export const useTaskTypesStore = defineStore('taskTypes', () => {
  const taskTypes = ref<TaskTypeDefinition[]>([])
  const loading = ref(false)
  const error = ref<string | null>(null)
  const activeTaskTypes = computed(() => taskTypes.value.filter((taskType) => taskType.active))

  async function fetchTaskTypes(projectId?: string | null) {
    loading.value = true
    error.value = null
    try {
      taskTypes.value = await listTaskTypes(projectId)
    } catch (caught) {
      error.value = caught instanceof Error ? caught.message : 'Unable to load task types.'
      throw caught
    } finally {
      loading.value = false
    }
  }

  return { taskTypes, activeTaskTypes, loading, error, fetchTaskTypes }
})
