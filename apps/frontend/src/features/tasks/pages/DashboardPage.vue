<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { RouterLink } from 'vue-router'
import {
  BellOutlined,
  SearchOutlined,
} from '@ant-design/icons-vue'
import { useTasks } from '../composables/useTasks'
import AppLayout from '../components/AppLayout.vue'
import { formatDateTime, isTaskOverdue } from '../utils/taskDates'
import type { Task } from '../types'

const { loading, visibleTasks, fetchTasks, fetchProjects } = useTasks()

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

interface DashboardQuickAction {
  label: string
  milestone: string
}

interface DashboardProjectStatus {
  name: string
  status: 'On track' | 'At risk' | 'Planning'
  progress: number
  color: string
}

interface DashboardRecentActivity {
  initials: string
  name: string
  action: string
  time: string
  color: string
}

interface DashboardPreview {
  quickActions: DashboardQuickAction[]
  weekdays: string[]
  projectStatus: DashboardProjectStatus[]
  recentActivity: DashboardRecentActivity[]
}

interface AnalyticsPreviewInsight {
  title: string
  description: string
  action: string
}

const mockOnlyDashboardPreview: DashboardPreview = {
  quickActions: [
    { label: 'Show blockers', milestone: 'M12' },
    { label: 'Rebalance workload', milestone: 'M12' },
    { label: 'Plan this week', milestone: 'M04' },
    { label: 'Ask Nova anything', milestone: 'M08' },
  ],
  weekdays: ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'],
  projectStatus: [
    { name: 'Atlas — Web App v2', status: 'On track', progress: 68, color: '#2563eb' },
    { name: 'Ion — Mobile App', status: 'At risk', progress: 34, color: '#7c3aed' },
    { name: 'Nova — AI Routing Engine', status: 'On track', progress: 82, color: '#0f766e' },
    { name: 'Echo — Customer Insights', status: 'Planning', progress: 12, color: '#c2410c' },
    { name: 'Orbit — Onboarding Flow', status: 'On track', progress: 54, color: '#16a34a' },
    { name: 'Pulse — Performance Sprint', status: 'On track', progress: 71, color: '#be185d' },
  ],
  recentActivity: [
    { initials: 'MP', name: 'Maya Patel', action: 'moved TSK-1027 to Review', time: '12m ago', color: '#4338ca' },
    { initials: 'JL', name: 'Jordan Lee', action: 'commented on Atlas blocker thread', time: '32m ago', color: '#0f766e' },
    { initials: 'SR', name: 'Sam Rivera', action: 'added risk note to TSK-1029', time: '1h ago', color: '#c2410c' },
    { initials: 'TP', name: 'Theo Park', action: 'created sprint follow-up tasks', time: '2h ago', color: '#1d4ed8' },
    { initials: 'KT', name: 'Kai Tanaka', action: 'updated delivery forecast', time: '3h ago', color: '#be185d' },
    { initials: 'NA', name: 'Nova AI', action: 'summarized blockers for Atlas', time: '4h ago', color: '#1e40af' },
  ],
}

const analyticsPreviewInsights = computed<AnalyticsPreviewInsight[]>(() => {
  const overdueTasks = visibleTasks.value.filter((task: Task) => isTaskOverdue(task))
  const doneThisSprint = realTaskMetrics.value.completed

  return [
    {
      title: overdueTasks.length ? 'Risk preview from current tasks' : 'Sprint flow preview',
      description: overdueTasks.length
        ? `${overdueTasks.length} overdue task(s) are visible locally. M12 will replace this placeholder with Relay-backed risk analytics.`
        : 'No overdue items detected in the current task list. M12 will replace this placeholder with real trend analytics.',
      action: 'Coming in M12',
    },
    {
      title: 'Workload analytics placeholder',
      description: `Team workload balancing needs the M12 analytics read model. Current completed task count is ${doneThisSprint}.`,
      action: 'Coming in M12',
    },
    {
      title: 'Nova insight placeholder',
      description: 'User-facing AI dashboard insight generation is implemented in M08, then replaced by real dashboard widgets in M12.',
      action: 'Coming in M08/M12',
    },
  ]
})

const myTasks = computed<Task[]>(() => visibleTasks.value.slice(0, 6))

onMounted(async () => {
  await fetchProjects()
  await fetchTasks()
})
</script>

<template>
  <AppLayout :task-count="realTaskMetrics.active">
    <header class="topbar">
      <div>
        <h1>Dashboard</h1>
        <p>Live task/project basics now; analytics widgets come in M12.</p>
      </div>
      <div class="topbar-actions">
        <a-input size="large" placeholder="Search tasks, projects, people...">
          <template #prefix><SearchOutlined /></template>
        </a-input>
        <a-button shape="circle"><BellOutlined /></a-button>
        <RouterLink to="/tasks"><a-button type="primary" size="large">+ New task</a-button></RouterLink>
      </div>
    </header>

    <section class="brief-card">
      <h2>Good morning <span>REAL TASK METRICS</span><span class="muted-pill">Dashboard analytics coming in M12</span></h2>
      <p>
        You have <strong>{{ realTaskMetrics.dueThisWeek }} task(s) due in the next 7 days</strong> and
        <strong>{{ realTaskMetrics.overdue }} overdue blocker(s)</strong> from the live task API.
      </p>
      <div class="chip-row">
        <a-button
          v-for="action in mockOnlyDashboardPreview.quickActions"
          :key="action.label"
          class="chip"
          disabled
        >
          {{ action.label }} <span>Coming in {{ action.milestone }}</span>
        </a-button>
      </div>
    </section>

    <section class="kpi-grid" aria-label="Live task metrics">
      <article class="kpi-card"><h3>Active Tasks</h3><strong>{{ realTaskMetrics.active }}</strong><p>Live non-completed tasks</p></article>
      <article class="kpi-card"><h3>Due Next 7 Days</h3><strong>{{ realTaskMetrics.dueThisWeek }}</strong><p>Live due dates</p></article>
      <article class="kpi-card"><h3>Overdue</h3><strong>{{ realTaskMetrics.overdue }}</strong><p>{{ realTaskMetrics.overdue ? 'Needs attention' : 'All clear' }}</p></article>
      <article class="kpi-card"><h3>Completed</h3><strong>{{ realTaskMetrics.completed }}</strong><p>Live completed tasks</p></article>
    </section>

    <section class="bottom-grid">
      <div class="left-column">
        <a-card class="tasks-card" :loading="loading" title="My Tasks">
          <template #extra><RouterLink to="/tasks">View all</RouterLink></template>
          <a-list :data-source="myTasks">
            <template #renderItem="{ item }: { item: Task }">
              <a-list-item>
                <a-list-item-meta :title="item.title" :description="`${item.projectId ?? 'PRJ'} · ${item.id}`" />
                <span class="due">{{ item.dueAt ? formatDateTime(item.dueAt) : 'No due date' }}</span>
              </a-list-item>
            </template>
          </a-list>
        </a-card>

        <a-card class="workload-card placeholder-card" title="Team Workload — This Week">
          <template #extra>
            <a-tag color="default">Mock preview · Coming in M12</a-tag>
          </template>
          <div class="legend"><span>To Do</span><span>In Progress</span><span>Done</span></div>
          <div class="weeklines">
            <div v-for="day in mockOnlyDashboardPreview.weekdays" :key="day" class="weekline"><i></i><label>{{ day }}</label></div>
          </div>
        </a-card>

        <a-card class="status-card placeholder-card" title="Project Status">
          <template #extra><a-tag color="default">Mock preview · Coming in M12</a-tag></template>
          <div class="project-row" v-for="project in mockOnlyDashboardPreview.projectStatus" :key="project.name">
            <p>{{ project.name }}</p>
            <div class="project-progress">
              <a-tag :color="project.status === 'At risk' ? 'orange' : project.status === 'Planning' ? 'default' : 'green'">{{ project.status }}</a-tag>
              <a-progress :percent="project.progress" :show-info="false" :stroke-color="project.color" />
              <span>{{ project.progress }}%</span>
            </div>
          </div>
        </a-card>
      </div>

      <div class="right-column">
        <a-card class="insights-card placeholder-card" title="AI Insights">
          <template #extra><a-tag color="blue">Coming in M08/M12</a-tag></template>
          <ul>
            <li v-for="insight in analyticsPreviewInsights" :key="insight.title"><h4>{{ insight.title }}</h4><p>{{ insight.description }}</p><span class="placeholder-link">{{ insight.action }}</span></li>
          </ul>
        </a-card>

        <a-card class="activity-card placeholder-card" title="Recent Activity">
          <template #extra><a-tag color="default">Mock preview · Coming in M12</a-tag></template>
          <div v-for="item in mockOnlyDashboardPreview.recentActivity" :key="item.name + item.time" class="activity-item">
            <div class="activity-avatar" :style="{ backgroundColor: item.color }">{{ item.initials }}</div>
            <div><strong>{{ item.name }}</strong> {{ item.action }}<p>{{ item.time }}</p></div>
          </div>
        </a-card>
      </div>
    </section>
  </AppLayout>
</template>

<style scoped>
.topbar { display:flex; justify-content:space-between; align-items:center; gap:16px; }
.topbar h1 { margin:0; }
.topbar p { margin:2px 0 0; color:#64748b; }
.topbar-actions { display:flex; gap:10px; align-items:center; }
.topbar-actions .ant-input-affix-wrapper { width:360px; }
.brief-card,.kpi-card,.tasks-card,.insights-card,.workload-card,.status-card,.activity-card { background:#fff; border:1px solid #e2e8f0; border-radius:14px; }
.brief-card { border-color:#c7d2fe; padding:20px; }
.brief-card h2 { margin:0; }
.brief-card span { font-size:12px; color:#4338ca; background:#eef2ff; border-radius:999px; padding:4px 8px; margin-left:8px; }
.brief-card .muted-pill { color:#64748b; background:#f1f5f9; }
.chip-row { display:flex; flex-wrap:wrap; gap:10px; }
.chip { border-radius:999px; }
.chip span { margin-left:4px; color:#64748b; }
.kpi-grid { display:grid; gap:12px; grid-template-columns:repeat(4,minmax(0,1fr)); }
.kpi-card { padding:16px; }
.kpi-card h3 { margin:0; font-size:12px; text-transform:uppercase; color:#64748b; }
.kpi-card strong { font-size:44px; }
.placeholder-card { position:relative; overflow:hidden; }
.placeholder-card::before { content:'Placeholder'; position:absolute; top:12px; right:-34px; transform:rotate(35deg); background:#f8fafc; color:#94a3b8; border:1px solid #e2e8f0; padding:2px 34px; font-size:11px; text-transform:uppercase; letter-spacing:.08em; }
.bottom-grid { display:grid; grid-template-columns:1.5fr 1fr; gap:12px; align-items:start; }
.left-column,.right-column { display:grid; gap:12px; }
.legend { display:flex; gap:10px; color:#64748b; font-size:12px; }
.weeklines { display:grid; grid-template-columns:repeat(7,1fr); gap:8px; padding-top:16px; }
.weekline i { display:block; height:5px; border-radius:999px; background:linear-gradient(90deg,#94a3b8,#2563eb,#16a34a); }
.weekline label { display:block; text-align:center; color:#64748b; font-size:12px; margin-top:8px; }
.project-row { display:flex; justify-content:space-between; gap:14px; padding:10px 0; border-bottom:1px solid #eef2f7; }
.project-row:last-child { border-bottom:none; }
.project-row p { margin:0; }
.project-progress { display:flex; align-items:center; gap:10px; min-width:320px; }
.project-progress .ant-progress { width:120px; }
.project-progress span { color:#475569; font-size:12px; }
.placeholder-link { color:#64748b; font-size:12px; font-weight:600; }
.activity-item { display:flex; gap:10px; padding:9px 0; border-bottom:1px solid #eef2f7; }
.activity-item:last-child { border-bottom:none; }
.activity-avatar { width:30px; height:30px; border-radius:50%; color:#fff; display:grid; place-items:center; font-size:11px; }
.activity-item p { margin:3px 0 0; color:#64748b; font-size:12px; }
@media (max-width: 1200px) { .kpi-grid,.bottom-grid { grid-template-columns:1fr; } }
@media (max-width: 860px) { .topbar { flex-direction:column; align-items:stretch; } .topbar-actions .ant-input-affix-wrapper, .project-progress { width:100%; min-width:0; } .weeklines{grid-template-columns:1fr} }
</style>
