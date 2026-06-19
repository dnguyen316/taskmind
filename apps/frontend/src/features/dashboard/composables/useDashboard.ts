import { computed, ref } from 'vue'
import { isApiError } from '../../../lib/apiError'
import { getDashboard } from '../api/dashboardApi'
import type { DashboardResponse } from '../types'

export function useDashboard() {
  const dashboard = ref<DashboardResponse | null>(null)
  const loading = ref(false)
  const errorMessage = ref('')
  const hasDashboardData = computed(
    () =>
      dashboard.value !== null &&
      (dashboard.value.myTasks.length > 0 || dashboard.value.activity.length > 0),
  )

  async function fetchDashboard() {
    loading.value = true
    errorMessage.value = ''
    try {
      dashboard.value = await getDashboard()
    } catch (error) {
      errorMessage.value = isApiError(error) ? error.message : 'Unable to load dashboard.'
      throw error
    } finally {
      loading.value = false
    }
  }

  return { dashboard, loading, errorMessage, hasDashboardData, fetchDashboard }
}
