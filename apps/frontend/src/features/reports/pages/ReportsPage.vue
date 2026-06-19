<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { BarChartOutlined, DownloadOutlined } from '@ant-design/icons-vue'
import AppLayout from '../../tasks/components/AppLayout.vue'
import ThroughputChart from '../components/ThroughputChart.vue'
import WorkloadChart from '../components/WorkloadChart.vue'
import { exportReportsPdf } from '../api/reportsApi'
import { useReports } from '../composables/useReports'
import type { ReportsRange } from '../types'

const { report, range, loading, errorMessage, hasReportData, fetchReport } = useReports('week')

const rangeOptions: { label: string; value: ReportsRange }[] = [
  { label: 'Week', value: 'week' },
  { label: 'Month', value: 'month' },
  { label: 'Quarter', value: 'quarter' },
]

const kpis = computed(() => report.value?.kpis)
const completionRate = computed(() => Math.round((kpis.value?.completionRate ?? 0) * 100))
const statusRows = computed(() => report.value?.statusSegments ?? [])
const projectRows = computed(() => report.value?.projectThroughput ?? [])
const trendRows = computed(() => report.value?.trends ?? [])
const workloadRows = computed(() => report.value?.assigneeWorkload ?? [])

const statusColumns = [
  { title: 'Status', dataIndex: 'status', key: 'status' },
  { title: 'Tasks', dataIndex: 'count', key: 'count', align: 'right' as const },
]
const projectColumns = [
  { title: 'Project', dataIndex: 'name', key: 'name' },
  { title: 'Created', dataIndex: 'tasksCreated', key: 'tasksCreated', align: 'right' as const },
  {
    title: 'Completed',
    dataIndex: 'tasksCompleted',
    key: 'tasksCompleted',
    align: 'right' as const,
  },
]
const workloadColumns = [
  { title: 'Assignee', dataIndex: 'userId', key: 'userId' },
  { title: 'Open tasks', dataIndex: 'openTasks', key: 'openTasks', align: 'right' as const },
]

function exportPdf() {
  if (report.value) exportReportsPdf(report.value)
}

function changeRange(nextRange: ReportsRange) {
  void fetchReport(nextRange).catch(() => undefined)
}

onMounted(() => {
  void fetchReport().catch(() => undefined)
})
</script>

<template>
  <AppLayout>
    <section class="reports-page">
      <a-card class="hero-card">
        <div>
          <div class="icon"><BarChartOutlined /></div>
          <a-tag color="blue">Live Core data</a-tag>
          <h1>Reports</h1>
          <p>
            Reports load from Core <code>GET /v1/reports</code> and reflect the analytics rollups
            available for the selected range.
          </p>
        </div>
        <div class="hero-actions">
          <a-segmented :value="range" :options="rangeOptions" @change="changeRange" />
          <a-button :disabled="!report" @click="exportPdf"
            ><template #icon><DownloadOutlined /></template>Export PDF</a-button
          >
        </div>
      </a-card>

      <a-alert
        v-if="errorMessage"
        type="error"
        show-icon
        :message="errorMessage"
        description="Refresh reports or try a different range."
      />

      <a-skeleton v-if="loading && !report" active :paragraph="{ rows: 6 }" />
      <template v-else>
        <a-empty v-if="!hasReportData" description="No report activity is available yet." />

        <div class="kpi-grid">
          <a-card><a-statistic title="Created" :value="kpis?.tasksCreated ?? 0" /></a-card>
          <a-card><a-statistic title="Completed" :value="kpis?.tasksCompleted ?? 0" /></a-card>
          <a-card><a-statistic title="Events" :value="kpis?.eventsIngested ?? 0" /></a-card>
          <a-card
            ><a-statistic title="Completion rate" :value="completionRate" suffix="%"
          /></a-card>
        </div>

        <div class="chart-grid">
          <a-card title="Throughput trend">
            <ThroughputChart :trends="trendRows" />
          </a-card>
          <a-card title="Workload balance">
            <WorkloadChart :rows="workloadRows" />
          </a-card>
        </div>

        <div class="report-grid">
          <a-card title="Status segments">
            <a-table
              :columns="statusColumns"
              :data-source="statusRows"
              :pagination="false"
              row-key="status"
              size="small"
            />
          </a-card>
          <a-card title="Project throughput">
            <a-table
              :columns="projectColumns"
              :data-source="projectRows"
              :pagination="false"
              row-key="projectId"
              size="small"
            />
          </a-card>
          <a-card title="Assignee workload">
            <a-table
              :columns="workloadColumns"
              :data-source="workloadRows"
              :pagination="false"
              row-key="userId"
              size="small"
            />
          </a-card>
          <a-card title="Team workload">
            <a-statistic title="Members" :value="report?.teamWorkload.members ?? 0" />
            <a-statistic title="Open tasks" :value="report?.teamWorkload.openTasks ?? 0" />
          </a-card>
        </div>
      </template>
    </section>
  </AppLayout>
</template>

<style scoped>
.reports-page {
  display: grid;
  gap: 16px;
}
.hero-card {
  border: 1px solid var(--tm-border);
  border-radius: 16px;
}
.hero-card :deep(.ant-card-body) {
  display: flex;
  gap: 16px;
  align-items: flex-start;
  justify-content: space-between;
}
.icon {
  width: 48px;
  height: 48px;
  margin-bottom: 12px;
  display: grid;
  color: var(--tm-accent-orange);
  font-size: 24px;
  background: var(--tm-surface-subtle);
  border-radius: 16px;
  place-items: center;
}
h1 {
  margin: 10px 0 8px;
}
p {
  max-width: 760px;
  color: var(--tm-text-muted);
}
code {
  padding: 1px 5px;
  background: var(--tm-surface-subtle);
  border: 1px solid var(--tm-border-soft);
  border-radius: 6px;
}
.hero-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  justify-content: flex-end;
}
.kpi-grid,
.chart-grid,
.report-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}
@media (max-width: 900px) {
  .hero-card :deep(.ant-card-body) {
    flex-direction: column;
  }
  .hero-actions {
    justify-content: flex-start;
  }
  .kpi-grid,
  .chart-grid,
  .report-grid {
    grid-template-columns: 1fr;
  }
}
</style>
