import { computed, ref } from 'vue'
import { getReports } from '../api/reportsApi'
import type { ReportsRange, ReportsResponse } from '../types'

export function useReports(initialRange: ReportsRange = 'week') {
  const report = ref<ReportsResponse | null>(null)
  const range = ref<ReportsRange>(initialRange)
  const loading = ref(false)
  const errorMessage = ref('')

  const hasReportData = computed(() => {
    const current = report.value
    if (!current) return false
    return (
      current.kpis.tasksCreated > 0 ||
      current.kpis.tasksCompleted > 0 ||
      current.kpis.eventsIngested > 0 ||
      current.trends.length > 0 ||
      current.projectThroughput.length > 0 ||
      current.assigneeWorkload.length > 0
    )
  })

  async function fetchReport(nextRange = range.value) {
    loading.value = true
    errorMessage.value = ''
    range.value = nextRange

    try {
      report.value = await getReports(nextRange)
      return report.value
    } catch (error: unknown) {
      errorMessage.value = error instanceof Error ? error.message : 'Failed to load reports.'
      throw error
    } finally {
      loading.value = false
    }
  }

  return { report, range, loading, errorMessage, hasReportData, fetchReport }
}
