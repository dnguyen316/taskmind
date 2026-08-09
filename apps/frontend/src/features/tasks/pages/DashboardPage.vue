<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import {
  ArrowRightOutlined,
  AppstoreOutlined,
  AuditOutlined,
  BellOutlined,
  CheckCircleOutlined,
  ClockCircleOutlined,
  FolderOpenOutlined,
  LineChartOutlined,
  RobotOutlined,
  RightOutlined,
  ThunderboltOutlined,
  UnorderedListOutlined,
} from '@ant-design/icons-vue'
import ActivitySearchAutocomplete from '../../activity/components/ActivitySearchAutocomplete.vue'
import { assistActivitySearch } from '../api/activitySearchApi'
import { useDashboard } from '../../dashboard/composables/useDashboard'
import type { DashboardActivitySnippet, DashboardTaskItem } from '../../dashboard/types'
import { useAuthStore } from '../../../stores/auth'
import AppLayout from '../components/AppLayout.vue'
import { formatDateTime } from '../utils/taskDates'
import type { ActivitySearchSuggestion } from '../types'

const { dashboard, loading, errorMessage, fetchDashboard } = useDashboard()
const authStore = useAuthStore()
const router = useRouter()
const searchQuery = ref('')
const dashboardSearch = ref<{ focusInput: () => void } | null>(null)
const aiSearchLoading = ref(false)
const aiSearchErrorMessage = ref('')
let aiSearchRequestId = 0
const dashboardKpis = computed(() => dashboard.value?.kpis)
const realTaskMetrics = computed(() => ({
  active: dashboardKpis.value?.openTasks ?? 0,
  completed: dashboardKpis.value?.completedTasks ?? 0,
  events: dashboardKpis.value?.eventsIngested ?? 0,
  completionRate: Math.round((dashboardKpis.value?.completionRate ?? 0) * 100),
}))
const myTasks = computed<DashboardTaskItem[]>(() => dashboard.value?.myTasks.slice(0, 6) ?? [])
const activity = computed(() => dashboard.value?.activity.slice(0, 5) ?? [])
const firstName = computed(() => authStore.currentUserDisplayName.trim().split(/\s+/)[0] || 'there')
const greeting = computed(() => {
  const hour = new Date().getHours()
  if (hour < 12) return 'Good morning'
  if (hour < 18) return 'Good afternoon'
  return 'Good evening'
})
const totalTasks = computed(() => realTaskMetrics.value.active + realTaskMetrics.value.completed)

const metricCards = computed(() => [
  {
    label: 'Open tasks',
    value: realTaskMetrics.value.active,
    note: `${totalTasks.value} total`,
    tone: 'blue',
    icon: ClockCircleOutlined,
  },
  {
    label: 'Completed',
    value: realTaskMetrics.value.completed,
    note: 'Finished in scope',
    tone: 'green',
    icon: CheckCircleOutlined,
  },
  {
    label: 'Completion rate',
    value: `${realTaskMetrics.value.completionRate}%`,
    note: 'Across all tasks',
    tone: 'purple',
    icon: LineChartOutlined,
  },
  {
    label: 'Events',
    value: realTaskMetrics.value.events,
    note: 'Activity recorded',
    tone: 'amber',
    icon: AuditOutlined,
  },
])

function submitDashboardSearch(value = searchQuery.value) {
  const query = value.trim()
  if (!query) return
  void router.push({ name: 'activity-search', query: { q: query } })
}

async function submitDashboardAiSearch(value = searchQuery.value) {
  const prompt = value.trim()
  if (!prompt) {
    dashboardSearch.value?.focusInput()
    return
  }

  searchQuery.value = value
  aiSearchRequestId += 1
  const requestId = aiSearchRequestId
  aiSearchLoading.value = true
  aiSearchErrorMessage.value = ''

  try {
    const proposal = await assistActivitySearch(prompt, prompt)
    if (requestId === aiSearchRequestId) {
      await router.push({ name: 'activity-search', query: { q: proposal.query } })
    }
  } catch (error: unknown) {
    if (requestId === aiSearchRequestId) {
      aiSearchErrorMessage.value =
        error instanceof Error ? error.message : 'Nova could not prepare this search.'
    }
  } finally {
    if (requestId === aiSearchRequestId) aiSearchLoading.value = false
  }
}

function selectDashboardSearchOption(value: string, recommendation?: ActivitySearchSuggestion) {
  searchQuery.value = value
  if (recommendation?.routeName && recommendation.entityId) {
    void router.push({ name: recommendation.routeName, params: { id: recommendation.entityId } })
    return
  }
  submitDashboardSearch(value)
}

function focusDashboardSearch(event: KeyboardEvent) {
  if ((event.metaKey || event.ctrlKey) && event.key.toLowerCase() === 'k') {
    event.preventDefault()
    dashboardSearch.value?.focusInput()
  }
}

function humanizeStatus(status: string) {
  return status
    .toLowerCase()
    .replace(/_/g, ' ')
    .replace(/(^|\s)\S/g, (letter: string) => letter.toUpperCase())
}

function statusTone(status: string) {
  const normalized = status.toUpperCase()
  if (normalized === 'DONE' || normalized === 'COMPLETED') return 'success'
  if (normalized.includes('PROGRESS')) return 'processing'
  if (normalized === 'BLOCKED') return 'error'
  return 'default'
}

function activitySummary(item: DashboardActivitySnippet) {
  const parts = []
  if (item.tasksCompleted > 0) parts.push(`${item.tasksCompleted} completed`)
  if (item.tasksCreated > 0) parts.push(`${item.tasksCreated} created`)
  if (item.eventsIngested > 0) parts.push(`${item.eventsIngested} events`)
  return parts.join(' · ') || 'No recorded changes'
}

onMounted(async () => {
  window.addEventListener('keydown', focusDashboardSearch)
  await fetchDashboard().catch(() => undefined)
})

onBeforeUnmount(() => {
  aiSearchRequestId += 1
  window.removeEventListener('keydown', focusDashboardSearch)
})
</script>

<template>
  <AppLayout :task-count="realTaskMetrics.active">
    <template #title>{{ greeting }}, {{ firstName }}</template>
    <template #subtitle>Here’s what needs your attention today.</template>
    <template #headerActions>
      <div class="dashboard-header-search">
        <div class="ai-search-control" :class="{ 'is-loading': aiSearchLoading }">
          <ActivitySearchAutocomplete
            ref="dashboardSearch"
            v-model:value="searchQuery"
            class="dashboard-search-autocomplete"
            appearance="ai"
            input-size="large"
            placeholder="Ask Nova to search workspace"
            :suggestion-limit="6"
            :show-shortcut-hint="true"
            show-view-all
            view-all-label="Search workspace"
            @select-suggestion="selectDashboardSearchOption"
            @submit-search="submitDashboardAiSearch"
            @view-all="submitDashboardSearch"
          />
          <a-button
            class="ask-nova-button"
            type="primary"
            size="large"
            :loading="aiSearchLoading"
            aria-label="Ask Nova to search"
            @click="submitDashboardAiSearch()"
          >
            <template #icon><RobotOutlined /></template>
            <span class="ask-nova-label">Ask Nova</span>
            <ArrowRightOutlined v-if="!aiSearchLoading" />
          </a-button>
        </div>
        <p v-if="aiSearchErrorMessage" class="ai-search-error" role="alert">
          {{ aiSearchErrorMessage }}
          <button type="button" @click="submitDashboardSearch()">Search without Nova</button>
        </p>
      </div>
      <a-button
        class="notification-button"
        shape="circle"
        size="large"
        disabled
        title="Notifications coming soon"
        aria-label="Notifications coming soon"
      >
        <BellOutlined />
      </a-button>
      <RouterLink to="/tasks"
        ><a-button type="primary" size="large">+ New task</a-button></RouterLink
      >
    </template>

    <section class="metric-rail tm-card-surface" aria-label="Task overview">
      <article v-for="metric in metricCards" :key="metric.label" class="metric-item">
        <span class="metric-icon" :class="`is-${metric.tone}`" aria-hidden="true">
          <component :is="metric.icon" />
        </span>
        <div class="metric-copy">
          <span>{{ metric.label }}</span
          ><strong>{{ metric.value }}</strong>
        </div>
        <small>{{ metric.note }}</small>
      </article>
    </section>

    <a-alert v-if="errorMessage" type="error" show-icon :message="errorMessage">
      <template #action><a-button size="small" @click="fetchDashboard">Retry</a-button></template>
    </a-alert>

    <section class="focus-panel" aria-labelledby="focus-title">
      <div class="focus-intro">
        <span class="focus-icon" aria-hidden="true"><ThunderboltOutlined /></span>
        <div>
          <h2 id="focus-title">Start with your highest-impact work</h2>
          <p>Choose a clear next step. TaskMind keeps the context close when you need it.</p>
        </div>
      </div>
      <nav class="focus-actions" aria-label="Recommended actions">
        <RouterLink to="/tasks" class="focus-action"
          ><UnorderedListOutlined /><span
            ><strong>Review tasks</strong><small>Choose what moves next</small></span
          ><RightOutlined
        /></RouterLink>
        <RouterLink to="/spec-breakdown" class="focus-action"
          ><AppstoreOutlined /><span
            ><strong>Break down a spec</strong><small>Turn scope into tasks</small></span
          ><RightOutlined
        /></RouterLink>
        <RouterLink to="/projects" class="focus-action"
          ><FolderOpenOutlined /><span
            ><strong>Open projects</strong><small>See active work</small></span
          ><RightOutlined
        /></RouterLink>
      </nav>
    </section>

    <section class="dashboard-grid">
      <article class="dashboard-panel tm-card-surface tasks-panel" aria-labelledby="my-tasks-title">
        <header class="panel-header">
          <div>
            <h2 id="my-tasks-title">My tasks</h2>
            <p>Your most recently updated work.</p>
          </div>
          <RouterLink to="/tasks" class="panel-link">View all tasks <RightOutlined /></RouterLink>
        </header>
        <div v-if="loading" class="panel-state" aria-live="polite">
          <a-spin /><span>Loading your work…</span>
        </div>
        <div v-else-if="myTasks.length === 0" class="panel-state empty-state">
          <CheckCircleOutlined /><strong>Your task list is clear</strong
          ><span>Create a task when you’re ready to start something new.</span
          ><RouterLink to="/tasks"><a-button type="primary">Create a task</a-button></RouterLink>
        </div>
        <div v-else class="task-table" role="table" aria-label="Recently updated tasks">
          <div class="task-row task-head" role="row">
            <span role="columnheader">Task</span><span role="columnheader">Project</span
            ><span role="columnheader">Status</span><span role="columnheader">Updated</span>
          </div>
          <RouterLink
            v-for="task in myTasks"
            :key="task.taskId"
            :to="{ name: 'task-detail', params: { id: task.taskId } }"
            class="task-row"
            role="row"
          >
            <span class="task-title" role="cell">{{ task.title }}</span
            ><span class="task-project" role="cell">{{ task.projectId ?? 'No project' }}</span
            ><span role="cell"
              ><a-tag :color="statusTone(task.status)">{{
                humanizeStatus(task.status)
              }}</a-tag></span
            ><span class="task-updated" role="cell">{{ formatDateTime(task.updatedAt) }}</span>
          </RouterLink>
        </div>
      </article>

      <article
        class="dashboard-panel tm-card-surface activity-panel"
        aria-labelledby="activity-title"
      >
        <header class="panel-header">
          <div>
            <h2 id="activity-title">Recent activity</h2>
            <p>A compact view of daily movement.</p>
          </div>
          <RouterLink to="/activity" class="panel-link" aria-label="View all activity"
            >View all <RightOutlined
          /></RouterLink>
        </header>
        <div v-if="loading" class="panel-state" aria-live="polite"><a-spin /></div>
        <div v-else-if="activity.length === 0" class="panel-state empty-state compact">
          <AuditOutlined /><strong>No activity yet</strong
          ><span>Task changes will appear here as your workspace gets moving.</span>
        </div>
        <ol v-else class="activity-list">
          <li v-for="item in activity" :key="item.date">
            <span class="activity-marker" aria-hidden="true"><AuditOutlined /></span>
            <div>
              <strong>{{ item.date }}</strong>
              <p>{{ activitySummary(item) }}</p>
            </div>
          </li>
        </ol>
      </article>
    </section>
  </AppLayout>
</template>

<style scoped>
.dashboard-header-search {
  position: relative;
  width: clamp(400px, 43vw, 620px);
  min-width: 0;
}
.ai-search-control {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 6px;
  align-items: center;
  width: 100%;
  padding: 5px;
  background: var(--tm-card-bg);
  border: 1px solid var(--tm-border);
  border-radius: 18px;
  box-shadow: 0 14px 36px rgba(28, 35, 64, 0.1);
  transition:
    border-color 180ms ease,
    box-shadow 180ms ease,
    transform 180ms ease;
}
.ai-search-control:focus-within {
  border-color: var(--tm-primary);
  box-shadow:
    0 0 0 4px color-mix(in srgb, var(--tm-primary) 13%, transparent),
    0 18px 42px rgba(28, 35, 64, 0.12);
  transform: translateY(-1px);
}
.ai-search-control.is-loading {
  border-color: var(--tm-primary-soft-border);
}
.dashboard-search-autocomplete {
  min-width: 0;
}
.ask-nova-button {
  display: inline-flex;
  gap: 8px;
  align-items: center;
  justify-content: center;
  min-width: 134px;
  height: 48px;
  padding-inline: 18px;
  font-size: 13px;
  font-weight: 650;
  border: 0;
  border-radius: 13px;
  box-shadow: 0 8px 18px color-mix(in srgb, var(--tm-primary) 28%, transparent);
}
.ai-search-error {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
  position: absolute;
  top: calc(100% + 7px);
  right: 0;
  z-index: 12;
  max-width: 100%;
  margin: 0;
  padding: 7px 10px;
  background: var(--tm-card-bg);
  border: 1px solid var(--tm-primary-soft-border);
  border-radius: 9px;
  box-shadow: var(--tm-shadow-md);
  color: var(--tm-warning);
  font-size: 12px;
}
.ai-search-error button {
  padding: 0;
  color: var(--tm-primary);
  font-weight: 650;
  cursor: pointer;
  background: none;
  border: 0;
}
.notification-button {
  border-color: var(--tm-border);
}
.metric-rail {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  padding: 0 12px;
  overflow: hidden;
}
.metric-item {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  gap: 0 14px;
  align-items: center;
  min-width: 0;
  padding: 20px 18px;
  border-right: 1px solid var(--tm-border-soft);
}
.metric-item:last-child {
  border-right: 0;
}
.metric-icon {
  display: grid;
  grid-row: 1 / span 2;
  width: 44px;
  height: 44px;
  font-size: 19px;
  border-radius: 12px;
  place-items: center;
}
.metric-icon.is-blue {
  color: #2563eb;
  background: #eaf1ff;
}
.metric-icon.is-green {
  color: #16803c;
  background: #e8f7ed;
}
.metric-icon.is-purple {
  color: #7048d7;
  background: #f1ecff;
}
.metric-icon.is-amber {
  color: #a86508;
  background: #fff5d9;
}
.metric-copy {
  display: flex;
  gap: 10px;
  align-items: baseline;
}
.metric-copy span,
.metric-item small {
  color: var(--tm-text-muted);
  font-size: 12px;
}
.metric-copy strong {
  order: -1;
  color: var(--tm-text);
  font-size: 26px;
  line-height: 1;
  letter-spacing: -0.04em;
}
.metric-item small {
  grid-column: 2;
  margin-top: 4px;
}
.focus-panel {
  display: grid;
  grid-template-columns: minmax(280px, 0.9fr) minmax(540px, 1.6fr);
  overflow: hidden;
  background: linear-gradient(105deg, #f7f9ff, #fff);
  border: 1px solid #cfd9ff;
  border-radius: var(--tm-radius);
  box-shadow: var(--tm-shadow-sm);
}
.focus-intro {
  display: flex;
  gap: 16px;
  align-items: center;
  padding: 24px;
}
.focus-icon {
  display: grid;
  flex: 0 0 48px;
  width: 48px;
  height: 48px;
  color: var(--tm-primary);
  font-size: 20px;
  background: var(--tm-primary-soft);
  border-radius: 16px;
  place-items: center;
}
.focus-panel h2,
.panel-header h2 {
  margin: 0;
  color: var(--tm-text);
  font-size: 17px;
  line-height: 1.3;
  letter-spacing: -0.02em;
}
.focus-panel p,
.panel-header p {
  margin: 6px 0 0;
  color: var(--tm-text-muted);
  font-size: 12px;
  line-height: 1.5;
}
.focus-actions {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  border-left: 1px solid var(--tm-border-soft);
}
.focus-action {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  gap: 10px;
  align-items: center;
  min-width: 0;
  padding: 20px 16px;
  color: var(--tm-primary);
  text-decoration: none;
  border-right: 1px solid var(--tm-border-soft);
  transition:
    background 160ms ease,
    color 160ms ease;
}
.focus-action:last-child {
  border-right: 0;
}
.focus-action:hover,
.focus-action:focus-visible {
  color: var(--tm-primary-hover);
  background: var(--tm-primary-soft);
}
.focus-action > :first-child {
  font-size: 20px;
}
.focus-action > :last-child {
  font-size: 11px;
}
.focus-action span {
  display: grid;
  gap: 3px;
  min-width: 0;
}
.focus-action strong,
.focus-action small {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.focus-action strong {
  font-size: 13px;
}
.focus-action small {
  color: var(--tm-text-muted);
  font-size: 11px;
}
.dashboard-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.55fr) minmax(320px, 0.8fr);
  gap: 16px;
  align-items: stretch;
}
.dashboard-panel {
  min-width: 0;
  overflow: hidden;
}
.panel-header {
  display: flex;
  gap: 16px;
  align-items: flex-start;
  justify-content: space-between;
  min-height: 76px;
  padding: 20px 22px 16px;
  border-bottom: 1px solid var(--tm-border-soft);
}
.panel-link {
  display: inline-flex;
  gap: 6px;
  align-items: center;
  margin-top: 2px;
  color: var(--tm-primary);
  font-size: 12px;
  font-weight: 650;
  white-space: nowrap;
}
.panel-state {
  display: flex;
  gap: 10px;
  align-items: center;
  justify-content: center;
  min-height: 240px;
  color: var(--tm-text-muted);
}
.empty-state {
  flex-direction: column;
  padding: 28px;
  text-align: center;
}
.empty-state > :first-child {
  color: var(--tm-success);
  font-size: 28px;
}
.empty-state strong {
  color: var(--tm-text);
}
.empty-state.compact {
  min-height: 220px;
}
.task-row {
  display: grid;
  grid-template-columns: minmax(190px, 1.5fr) minmax(110px, 0.85fr) 110px minmax(150px, 0.8fr);
  gap: 14px;
  align-items: center;
  min-height: 60px;
  padding: 10px 22px;
  color: var(--tm-text);
  text-decoration: none;
  border-bottom: 1px solid var(--tm-border-soft);
  transition: background 140ms ease;
}
.task-row:last-child {
  border-bottom: 0;
}
.task-row:not(.task-head):hover,
.task-row:not(.task-head):focus-visible {
  background: var(--tm-surface-subtle);
}
.task-head {
  min-height: 38px;
  color: var(--tm-text-soft);
  font-size: 11px;
  font-weight: 650;
  background: var(--tm-surface-subtle);
}
.task-title,
.task-project,
.task-updated {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.task-title {
  font-size: 13px;
  font-weight: 650;
}
.task-project,
.task-updated {
  color: var(--tm-text-muted);
  font-size: 11px;
}
.task-row :deep(.ant-tag) {
  margin: 0;
  font-size: 11px;
}
.activity-list {
  position: relative;
  display: grid;
  gap: 0;
  margin: 0;
  padding: 12px 22px 18px;
  list-style: none;
}
.activity-list::before {
  position: absolute;
  top: 32px;
  bottom: 38px;
  left: 38px;
  width: 1px;
  content: '';
  background: var(--tm-border);
}
.activity-list li {
  position: relative;
  z-index: 1;
  display: grid;
  grid-template-columns: 34px minmax(0, 1fr);
  gap: 12px;
  align-items: center;
  min-height: 64px;
}
.activity-marker {
  display: grid;
  width: 34px;
  height: 34px;
  color: var(--tm-primary);
  font-size: 14px;
  background: var(--tm-primary-soft);
  border: 4px solid var(--tm-card-bg);
  border-radius: 999px;
  place-items: center;
}
.activity-list strong {
  color: var(--tm-text);
  font-size: 13px;
}
.activity-list p {
  margin: 4px 0 0;
  color: var(--tm-text-muted);
  font-size: 11px;
  line-height: 1.45;
}
@media (max-width: 1380px) {
  .focus-panel {
    grid-template-columns: 1fr;
  }
  .focus-actions {
    border-top: 1px solid var(--tm-border-soft);
    border-left: 0;
  }
}
@media (max-width: 1100px) {
  .metric-rail {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
  .metric-item:nth-child(2) {
    border-right: 0;
  }
  .metric-item:nth-child(-n + 2) {
    border-bottom: 1px solid var(--tm-border-soft);
  }
  .dashboard-grid {
    grid-template-columns: 1fr;
  }
}
@media (max-width: 720px) {
  .metric-rail {
    grid-template-columns: 1fr;
    padding: 0 14px;
  }
  .metric-item,
  .metric-item:nth-child(3) {
    border-right: 0;
    border-bottom: 1px solid var(--tm-border-soft);
  }
  .metric-item:last-child {
    border-bottom: 0;
  }
  .focus-actions {
    grid-template-columns: 1fr;
  }
  .focus-action {
    border-right: 0;
    border-bottom: 1px solid var(--tm-border-soft);
  }
  .focus-action:last-child {
    border-bottom: 0;
  }
  .task-row {
    grid-template-columns: minmax(0, 1fr) auto;
    gap: 6px 12px;
  }
  .task-head {
    display: none;
  }
  .task-project {
    grid-column: 1;
  }
  .task-updated {
    grid-column: 2;
    grid-row: 2;
    text-align: right;
  }
  .panel-header {
    padding-inline: 18px;
  }
}
@media (prefers-reduced-motion: reduce) {
  .ai-search-control {
    transition: none;
  }
  .ai-search-control:focus-within {
    transform: none;
  }
}
</style>
