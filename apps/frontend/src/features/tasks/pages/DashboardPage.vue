<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import { BellOutlined, SearchOutlined } from '@ant-design/icons-vue'
import { useTasks } from '../composables/useTasks'
import AppLayout from '../components/AppLayout.vue'
import { formatDateTime, isTaskOverdue } from '../utils/taskDates'
import type { Task } from '../types'

const { loading, visibleTasks, fetchTasks, fetchProjects } = useTasks()
const router = useRouter()
const searchQuery = ref('')

const startOfToday = () => {
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  return today
}

const endOfNextSevenDays = () => {
  const end = startOfToday()
  end.setDate(end.getDate() + 7)
  end.setHours(23, 59, 59, 999)
  return end
}

const realTaskMetrics = computed(() => {
  const today = startOfToday()
  const nextSevenDays = endOfNextSevenDays()

  return {
    active: visibleTasks.value.filter((task: Task) => task.status !== 'DONE').length,
    dueThisWeek: visibleTasks.value.filter((task: Task) => {
      if (!task.dueAt) return false
      const dueAt = new Date(task.dueAt)
      return dueAt >= today && dueAt <= nextSevenDays
    }).length,
    overdue: visibleTasks.value.filter((task: Task) => isTaskOverdue(task)).length,
    completed: visibleTasks.value.filter((task: Task) => task.status === 'DONE').length,
  }
})

interface DashboardRoadmapCard {
  title: string
  milestone: string
  description: string
}

const upcomingDashboardFeatures: DashboardRoadmapCard[] = [
  {
    title: 'Team workload',
    milestone: 'M12',
    description:
      'Capacity and balancing charts will appear here when dashboard analytics are live.',
  },
  {
    title: 'Project health',
    milestone: 'M12',
    description: 'Project status cards will use Relay-backed analytics instead of sample progress.',
  },
  {
    title: 'AI insights',
    milestone: 'M08/M12',
    description: 'Nova summaries and dashboard insight widgets are coming soon.',
  },
  {
    title: 'Notifications',
    milestone: 'M11',
    description: 'Notification shortcuts are disabled until alert delivery is implemented.',
  },
]

const myTasks = computed<Task[]>(() => visibleTasks.value.slice(0, 6))

function submitDashboardSearch(value = searchQuery.value) {
  const query = value.trim()

  if (!query) {
    return
  }

  void router.push({ name: 'activity-search', query: { q: query } })
}

onMounted(async () => {
  await fetchProjects()
  await fetchTasks()
})
</script>

<template>
  <AppLayout :task-count="realTaskMetrics.active">
    <template #title>Dashboard</template>
    <template #subtitle>Live task/project basics now; analytics widgets come in M12.</template>
    <template #headerActions>
      <div class="dashboard-search">
        <a-input-search
          v-model:value="searchQuery"
          size="large"
          placeholder="Search activity, tasks, projects..."
          enter-button
          allow-clear
          @search="submitDashboardSearch"
        >
          <template #prefix><SearchOutlined /></template>
        </a-input-search>
      </div>
      <a-button shape="circle" disabled title="Notifications coming in a later milestone"
        ><BellOutlined
      /></a-button>
      <RouterLink to="/tasks"
        ><a-button type="primary" size="large">+ New task</a-button></RouterLink
      >
    </template>

    <section class="brief-card tm-card-surface">
      <div class="brief-header">
        <div class="brief-avatar" aria-hidden="true">AI</div>
        <div class="brief-copy">
          <div class="brief-title-row">
            <h2>Good morning</h2>
            <span class="brief-chip primary">REAL TASK METRICS</span>
            <span class="brief-chip muted">Dashboard analytics coming in M12</span>
          </div>
          <p>
            You have
            <strong>{{ realTaskMetrics.dueThisWeek }} task(s) due in the next 7 days</strong>
            and
            <strong>{{ realTaskMetrics.overdue }} overdue blocker(s)</strong>
            from the live task API.
          </p>
        </div>
      </div>
      <div class="live-actions">
        <RouterLink to="/tasks"><a-button type="primary">Review live tasks</a-button></RouterLink>
        <RouterLink to="/projects"><a-button>Open projects</a-button></RouterLink>
      </div>
    </section>

    <section class="kpi-grid" aria-label="Live task metrics">
      <article class="kpi-card tm-card-surface">
        <h3>Active Tasks</h3>
        <strong>{{ realTaskMetrics.active }}</strong>
        <p>Live non-completed tasks</p>
      </article>
      <article class="kpi-card tm-card-surface">
        <h3>Due Next 7 Days</h3>
        <strong>{{ realTaskMetrics.dueThisWeek }}</strong>
        <p>Live due dates</p>
      </article>
      <article class="kpi-card tm-card-surface">
        <h3>Overdue</h3>
        <strong>{{ realTaskMetrics.overdue }}</strong>
        <p>{{ realTaskMetrics.overdue ? 'Needs attention' : 'All clear' }}</p>
      </article>
      <article class="kpi-card tm-card-surface">
        <h3>Completed</h3>
        <strong>{{ realTaskMetrics.completed }}</strong>
        <p>Live completed tasks</p>
      </article>
    </section>

    <section class="live-dashboard-grid">
      <div class="live-column">
        <a-card class="tasks-card tm-card-surface" :loading="loading" title="My Tasks">
          <template #extra><RouterLink to="/tasks">View all</RouterLink></template>
          <a-list class="tm-list-surface" :data-source="myTasks">
            <template #renderItem="{ item }: { item: Task }">
              <a-list-item>
                <a-list-item-meta
                  :title="item.title"
                  :description="`${item.projectId ?? 'PRJ'} · ${item.id}`"
                />
                <span class="due">{{
                  item.dueAt ? formatDateTime(item.dueAt) : 'No due date'
                }}</span>
              </a-list-item>
            </template>
          </a-list>
        </a-card>
      </div>
    </section>

    <a-collapse class="upcoming-collapse" ghost>
      <a-collapse-panel key="upcoming-dashboard-features" header="Upcoming features">
        <p class="upcoming-intro">
          These dashboard areas are intentionally disabled until their backend data is live. No
          sample people, projects, or activity are shown in primary dashboard areas.
        </p>
        <div class="roadmap-grid">
          <article
            v-for="feature in upcomingDashboardFeatures"
            :key="feature.title"
            class="roadmap-card tm-card-surface"
            aria-disabled="true"
          >
            <div>
              <h3>{{ feature.title }}</h3>
              <p>{{ feature.description }}</p>
            </div>
            <a-tag color="default">Coming soon · {{ feature.milestone }}</a-tag>
          </article>
        </div>
      </a-collapse-panel>
    </a-collapse>
  </AppLayout>
</template>

<style scoped>
.dashboard-search .ant-input-affix-wrapper {
  width: 360px;
  color: var(--tm-text);
  background: var(--tm-card-bg);
  border-color: var(--tm-border);
}

.brief-card {
  padding: 20px;
  border-color: var(--tm-primary-soft-border);
}

.brief-header {
  display: flex;
  gap: 14px;
  align-items: flex-start;
}

.brief-avatar {
  display: grid;
  flex: 0 0 44px;
  width: 44px;
  height: 44px;
  place-items: center;
  color: var(--tm-accent-indigo);
  font-size: 13px;
  font-weight: 700;
  background: var(--tm-primary-soft);
  border: 1px solid var(--tm-primary-soft-border);
  border-radius: 14px;
}

.brief-copy {
  display: grid;
  gap: 8px;
  min-width: 0;
}

.brief-title-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
}

.brief-card h2 {
  margin: 0;
  color: var(--tm-text);
}

.brief-chip {
  padding: 4px 8px;
  font-size: 12px;
  border-radius: 999px;
}

.brief-chip.primary {
  color: var(--tm-accent-indigo);
  background: var(--tm-primary-soft);
}

.brief-chip.muted {
  color: var(--tm-muted);
  background: var(--tm-surface-subtle);
}

.brief-card p,
.kpi-card p {
  color: var(--tm-text-muted);
}

.live-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.kpi-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.kpi-card {
  padding: 16px;
}

.kpi-card h3 {
  margin: 0;
  color: var(--tm-muted);
  font-size: 12px;
  text-transform: uppercase;
}

.kpi-card strong {
  color: var(--tm-text);
  font-size: 44px;
}

.live-dashboard-grid {
  display: grid;
  gap: 12px;
  align-items: start;
}

.live-column {
  display: grid;
  gap: 12px;
}

.due {
  color: var(--tm-text-muted);
  font-size: 12px;
}

.upcoming-collapse {
  border: 1px solid var(--tm-border);
  border-radius: 18px;
}

.upcoming-intro {
  margin: 0 0 14px;
  color: var(--tm-text-muted);
}

.roadmap-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.roadmap-card {
  display: flex;
  flex-direction: column;
  gap: 14px;
  justify-content: space-between;
  min-height: 150px;
  padding: 16px;
  opacity: 0.72;
  filter: grayscale(0.2);
}

.roadmap-card h3 {
  margin: 0 0 8px;
  color: var(--tm-text);
}

.roadmap-card p {
  margin: 0;
  color: var(--tm-text-muted);
}

@media (max-width: 1200px) {
  .kpi-grid,
  .roadmap-grid {
    grid-template-columns: 1fr;
  }
}
</style>
