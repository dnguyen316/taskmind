<script setup lang="ts">
import { computed, h, onMounted, onScopeDispose, ref, watch } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import { BellOutlined, SearchOutlined } from '@ant-design/icons-vue'
import { useDashboard } from '../../dashboard/composables/useDashboard'
import { suggestActivitySearch } from '../api/activitySearchApi'
import AppLayout from '../components/AppLayout.vue'
import { formatDateTime } from '../utils/taskDates'
import type { DashboardTaskItem } from '../../dashboard/types'

const { dashboard, loading, errorMessage, fetchDashboard } = useDashboard()
const router = useRouter()
const searchQuery = ref('')
const searchFocused = ref(false)
const searchSuggestions = ref<string[]>([])
const searchSuggestionsLoading = ref(false)
const searchSuggestionsErrorMessage = ref('')
let searchSuggestionTimer: ReturnType<typeof setTimeout> | undefined
let searchSuggestionRequestId = 0

const DASHBOARD_SEARCH_MIN_LENGTH = 2
const DASHBOARD_SEARCH_SUGGESTION_LIMIT = 6
const VIEW_ALL_OPTION_PREFIX = '__taskmind_view_all__:'

const dashboardKpis = computed(() => dashboard.value?.kpis)
const realTaskMetrics = computed(() => ({
  active: dashboardKpis.value?.openTasks ?? 0,
  completed: dashboardKpis.value?.completedTasks ?? 0,
  events: dashboardKpis.value?.eventsIngested ?? 0,
  completionRate: Math.round((dashboardKpis.value?.completionRate ?? 0) * 100),
}))
const myTasks = computed<DashboardTaskItem[]>(() => dashboard.value?.myTasks.slice(0, 6) ?? [])
const activity = computed(() => dashboard.value?.activity ?? [])
const trimmedSearchQuery = computed(() => searchQuery.value.trim())
const showSearchDropdown = computed(
  () => searchFocused.value && trimmedSearchQuery.value.length >= DASHBOARD_SEARCH_MIN_LENGTH,
)
const searchSuggestionOptions = computed(() => {
  const query = trimmedSearchQuery.value

  if (query.length < DASHBOARD_SEARCH_MIN_LENGTH) {
    return []
  }

  if (searchSuggestionsLoading.value) {
    return [{ value: 'dashboard-search-loading', label: 'Searching…', disabled: true }]
  }

  if (searchSuggestionsErrorMessage.value) {
    return [
      {
        value: 'dashboard-search-error',
        label: 'Suggestion request failed.',
        disabled: true,
      },
      viewAllOption(query),
    ]
  }

  const suggestionOptions = searchSuggestions.value.map((suggestion) => ({
    value: suggestion,
    label: suggestion,
  }))

  if (suggestionOptions.length === 0) {
    return [
      { value: 'dashboard-search-empty', label: 'No matches found.', disabled: true },
      viewAllOption(query),
    ]
  }

  return [...suggestionOptions, viewAllOption(query)]
})

function viewAllOption(query: string) {
  return {
    value: `${VIEW_ALL_OPTION_PREFIX}${query}`,
    label: h('strong', `View all results for “${query}”`),
  }
}

async function loadDashboardSearchSuggestions() {
  const query = trimmedSearchQuery.value
  searchSuggestionRequestId += 1
  const requestId = searchSuggestionRequestId

  if (query.length < DASHBOARD_SEARCH_MIN_LENGTH) {
    searchSuggestions.value = []
    searchSuggestionsErrorMessage.value = ''
    searchSuggestionsLoading.value = false
    return
  }

  searchSuggestionsLoading.value = true
  searchSuggestionsErrorMessage.value = ''

  try {
    const suggestions = await suggestActivitySearch({
      query,
      size: DASHBOARD_SEARCH_SUGGESTION_LIMIT,
    })
    if (requestId === searchSuggestionRequestId) {
      searchSuggestions.value = suggestions
    }
  } catch (error: unknown) {
    if (requestId === searchSuggestionRequestId) {
      searchSuggestions.value = []
      searchSuggestionsErrorMessage.value =
        error instanceof Error ? error.message : 'Failed to load activity suggestions.'
    }
  } finally {
    if (requestId === searchSuggestionRequestId) {
      searchSuggestionsLoading.value = false
    }
  }
}

watch(searchQuery, () => {
  if (searchSuggestionTimer) {
    clearTimeout(searchSuggestionTimer)
  }
  searchSuggestionTimer = setTimeout(() => {
    void loadDashboardSearchSuggestions()
  }, 250)
})

onScopeDispose(() => {
  if (searchSuggestionTimer) {
    clearTimeout(searchSuggestionTimer)
  }
})

function submitDashboardSearch(value = searchQuery.value) {
  const query = value.trim()

  if (!query) {
    return
  }

  searchFocused.value = false
  void router.push({ name: 'activity-search', query: { q: query } })
}

function selectDashboardSearchOption(value: string) {
  if (value.startsWith(VIEW_ALL_OPTION_PREFIX)) {
    submitDashboardSearch(value.slice(VIEW_ALL_OPTION_PREFIX.length))
    return
  }

  searchQuery.value = value
  submitDashboardSearch(value)
}

function closeSearchDropdown() {
  window.setTimeout(() => {
    searchFocused.value = false
  }, 150)
}

onMounted(async () => {
  await fetchDashboard().catch(() => undefined)
})
</script>

<template>
  <AppLayout :task-count="realTaskMetrics.active">
    <template #title>Dashboard</template>
    <template #subtitle>Live Core dashboard aggregation from analytics read models.</template>
    <template #headerActions>
      <div class="dashboard-search">
        <a-auto-complete
          v-model:value="searchQuery"
          class="dashboard-search-autocomplete"
          :options="searchSuggestionOptions"
          :open="showSearchDropdown"
          allow-clear
          @focus="searchFocused = true"
          @blur="closeSearchDropdown"
          @select="selectDashboardSearchOption"
        >
          <a-input
            size="large"
            placeholder="Search activity, tasks, projects..."
            @press-enter="submitDashboardSearch()"
          >
            <template #prefix><SearchOutlined /></template>
          </a-input>
        </a-auto-complete>
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
            <span class="brief-chip primary">LIVE CORE DASHBOARD</span>
            <span class="brief-chip muted">Cached aggregation</span>
          </div>
          <p>
            You have
            <strong>{{ realTaskMetrics.active }} open task(s)</strong>,
            <strong>{{ realTaskMetrics.completed }} completed task(s)</strong>, and
            <strong>{{ realTaskMetrics.events }} analytics event(s)</strong> in the dashboard
            rollup.
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
        <h3>Open Tasks</h3>
        <strong>{{ realTaskMetrics.active }}</strong>
        <p>Core dashboard aggregation</p>
      </article>
      <article class="kpi-card tm-card-surface">
        <h3>Completed</h3>
        <strong>{{ realTaskMetrics.completed }}</strong>
        <p>Finished tasks in scope</p>
      </article>
      <article class="kpi-card tm-card-surface">
        <h3>Events</h3>
        <strong>{{ realTaskMetrics.events }}</strong>
        <p>Analytics events ingested</p>
      </article>
      <article class="kpi-card tm-card-surface">
        <h3>Completion Rate</h3>
        <strong>{{ realTaskMetrics.completionRate }}%</strong>
        <p>Completed / total tasks</p>
      </article>
    </section>

    <a-alert v-if="errorMessage" type="error" show-icon :message="errorMessage" />

    <section class="live-dashboard-grid">
      <div class="live-column">
        <a-card class="tasks-card tm-card-surface" :loading="loading" title="My Tasks">
          <template #extra><RouterLink to="/tasks">View all</RouterLink></template>
          <a-list class="tm-list-surface" :data-source="myTasks">
            <template #renderItem="{ item }: { item: DashboardTaskItem }">
              <a-list-item>
                <a-list-item-meta
                  :title="item.title"
                  :description="`${item.projectId ?? 'No project'} · ${item.taskId}`"
                />
                <span class="due">{{ formatDateTime(item.updatedAt) }}</span>
              </a-list-item>
            </template>
          </a-list>
        </a-card>
      </div>
    </section>

    <a-card class="tasks-card tm-card-surface" title="Activity snippet">
      <a-list class="tm-list-surface" :data-source="activity">
        <template #renderItem="{ item }">
          <a-list-item>
            <a-list-item-meta
              :title="item.date"
              :description="`Created ${item.tasksCreated} · Completed ${item.tasksCompleted} · Events ${item.eventsIngested}`"
            />
          </a-list-item>
        </template>
      </a-list>
    </a-card>
  </AppLayout>
</template>

<style scoped>
.dashboard-search-autocomplete {
  width: 360px;
}

.dashboard-search .ant-input-affix-wrapper {
  width: 100%;
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
