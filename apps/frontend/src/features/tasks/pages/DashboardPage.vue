<script setup>
import { computed, onMounted } from 'vue'
import {
  BellOutlined,
  CalendarOutlined,
  InboxOutlined,
  SearchOutlined,
  MessageOutlined,
  BarChartOutlined,
} from '@ant-design/icons-vue'
import { useTasks } from '../composables/useTasks'
import AppLayout from '../components/AppLayout.vue'
import { formatDateTime, isTaskOverdue } from '../utils/taskDates'

const { loading, visibleTasks, fetchTasks, fetchProjects } = useTasks()

const taskMetrics = computed(() => ({
  active: visibleTasks.value.length,
  dueThisWeek: visibleTasks.value.filter((task) => task.dueAt).length,
  overdue: visibleTasks.value.filter((task) => isTaskOverdue(task)).length,
  completed: visibleTasks.value.filter((task) => task.status === 'DONE').length,
}))

const quickActions = ['Show blockers', 'Rebalance workload', 'Plan this week', 'Ask Nova anything']
const weekdays = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun']

const projectStatus = [
  { name: 'Atlas — Web App v2', status: 'On track', progress: 68, color: '#2563eb' },
  { name: 'Ion — Mobile App', status: 'At risk', progress: 34, color: '#7c3aed' },
  { name: 'Nova — AI Routing Engine', status: 'On track', progress: 82, color: '#0f766e' },
  { name: 'Echo — Customer Insights', status: 'Planning', progress: 12, color: '#c2410c' },
  { name: 'Orbit — Onboarding Flow', status: 'On track', progress: 54, color: '#16a34a' },
  { name: 'Pulse — Performance Sprint', status: 'On track', progress: 71, color: '#be185d' },
]

const recentActivity = [
  { initials: 'MP', name: 'Maya Patel', action: 'moved TSK-1027 to Review', time: '12m ago', color: '#4338ca' },
  { initials: 'JL', name: 'Jordan Lee', action: 'commented on Atlas blocker thread', time: '32m ago', color: '#0f766e' },
  { initials: 'SR', name: 'Sam Rivera', action: 'added risk note to TSK-1029', time: '1h ago', color: '#c2410c' },
  { initials: 'TP', name: 'Theo Park', action: 'created sprint follow-up tasks', time: '2h ago', color: '#1d4ed8' },
  { initials: 'KT', name: 'Kai Tanaka', action: 'updated delivery forecast', time: '3h ago', color: '#be185d' },
  { initials: 'NA', name: 'Nova AI', action: 'summarized blockers for Atlas', time: '4h ago', color: '#1e40af' },
]

const aiInsights = computed(() => {
  const overdueTasks = visibleTasks.value.filter((task) => isTaskOverdue(task))
  const doneThisSprint = visibleTasks.value.filter((task) => task.status === 'DONE').length

  return [
    {
      title: overdueTasks.length ? 'Atlas at risk of slipping' : 'Sprint flow healthy',
      description: overdueTasks.length
        ? `${overdueTasks.length} urgent tasks share dependencies. Resolving blockers unlocks downstream work.`
        : 'No overdue items detected. Keep momentum by pulling one next-highest priority task.',
      action: overdueTasks.length ? 'Show blockers' : 'Pull into sprint',
    },
    {
      title: 'Workload imbalance detected',
      description: `A few owners hold most active items. You closed ${doneThisSprint} tasks this sprint—consider redistributing urgent work.`,
      action: 'Reassign',
    },
    {
      title: 'Velocity trend up 12% w/w',
      description: `Pulse sprint is outperforming forecast with ${taskMetrics.value.active} tasks currently in flight.`,
      action: 'Pull into sprint',
    },
  ]
})

const myTasks = computed(() => visibleTasks.value.slice(0, 6))

onMounted(async () => {
  await fetchProjects()
  await fetchTasks()
})
</script>

<template>
  <AppLayout :task-count="taskMetrics.active">

      <header class="topbar">
        <div>
          <h1>Dashboard</h1>
          <p>What's happening across your team today</p>
        </div>
        <div class="topbar-actions">
          <a-input size="large" placeholder="Search tasks, projects, people...">
            <template #prefix><SearchOutlined /></template>
          </a-input>
          <a-button shape="circle"><BellOutlined /></a-button>
          <a-button type="primary" size="large">+ New</a-button>
        </div>
      </header>

      <section class="brief-card">
        <h2>Good morning, Alex <span>NOVA AI · DAILY BRIEF</span></h2>
        <p>
          You have <strong>{{ taskMetrics.dueThisWeek }} tasks due this week</strong> and
          <strong>{{ taskMetrics.overdue }} urgent blocker(s)</strong>. Keep sprint velocity strong by pulling the
          next scoped task today.
        </p>
        <div class="chip-row">
          <a-button v-for="action in quickActions" :key="action" class="chip">{{ action }}</a-button>
        </div>
      </section>

      <section class="kpi-grid">
        <article class="kpi-card"><h3>Active Tasks</h3><strong>{{ taskMetrics.active }}</strong><p>In progress now</p></article>
        <article class="kpi-card"><h3>Due This Week</h3><strong>{{ taskMetrics.dueThisWeek }}</strong><p>Prioritize today</p></article>
        <article class="kpi-card"><h3>Overdue</h3><strong>{{ taskMetrics.overdue }}</strong><p>{{ taskMetrics.overdue ? 'Needs attention' : 'All clear' }}</p></article>
        <article class="kpi-card"><h3>Completed</h3><strong>{{ taskMetrics.completed }}</strong><p>Done this sprint</p></article>
      </section>

      <section class="bottom-grid">
        <div class="left-column">
          <a-card class="tasks-card" :loading="loading" title="My Tasks">
            <template #extra><a href="#">View all</a></template>
            <a-list :data-source="myTasks">
              <template #renderItem="{ item }">
                <a-list-item>
                  <a-list-item-meta :title="item.title" :description="`${item.projectKey ?? 'PRJ'} · ${item.id}`" />
                  <span class="due">{{ item.dueAt ? formatDateTime(item.dueAt) : 'No due date' }}</span>
                </a-list-item>
              </template>
            </a-list>
          </a-card>

          <a-card class="workload-card" title="Team Workload — This Week">
            <template #extra>
              <div class="legend"><span>To Do</span><span>In Progress</span><span>Done</span></div>
            </template>
            <div class="weeklines">
              <div v-for="day in weekdays" :key="day" class="weekline"><i></i><label>{{ day }}</label></div>
            </div>
          </a-card>

          <a-card class="status-card" title="Project Status">
            <template #extra><a href="#">View all</a></template>
            <div class="project-row" v-for="project in projectStatus" :key="project.name">
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
          <a-card class="insights-card" title="AI Insights">
            <template #extra><a-tag color="blue">{{ aiInsights.length }} NEW</a-tag></template>
            <ul>
              <li v-for="insight in aiInsights" :key="insight.title"><h4>{{ insight.title }}</h4><p>{{ insight.description }}</p><a href="#">{{ insight.action }}</a></li>
            </ul>
          </a-card>

          <a-card class="activity-card" title="Recent Activity">
            <div v-for="item in recentActivity" :key="item.name + item.time" class="activity-item">
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
.chip-row { display:flex; flex-wrap:wrap; gap:10px; } .chip{border-radius:999px}
.kpi-grid { display:grid; gap:12px; grid-template-columns:repeat(4,minmax(0,1fr)); }
.kpi-card { padding:16px; } .kpi-card h3{margin:0;font-size:12px;text-transform:uppercase;color:#64748b}.kpi-card strong{font-size:44px}
.bottom-grid { display:grid; grid-template-columns:1.5fr 1fr; gap:12px; align-items:start; }
.left-column,.right-column{display:grid;gap:12px}
.legend{display:flex;gap:10px;color:#64748b;font-size:12px}
.weeklines{display:grid;grid-template-columns:repeat(7,1fr);gap:8px;padding-top:16px}
.weekline i{display:block;height:5px;border-radius:999px;background:linear-gradient(90deg,#94a3b8,#2563eb,#16a34a)} .weekline label{display:block;text-align:center;color:#64748b;font-size:12px;margin-top:8px}
.project-row{display:flex;justify-content:space-between;gap:14px;padding:10px 0;border-bottom:1px solid #eef2f7}.project-row:last-child{border-bottom:none}.project-row p{margin:0}
.project-progress{display:flex;align-items:center;gap:10px;min-width:320px}.project-progress .ant-progress{width:120px}.project-progress span{color:#475569;font-size:12px}
.activity-item{display:flex;gap:10px;padding:9px 0;border-bottom:1px solid #eef2f7}.activity-item:last-child{border-bottom:none}.activity-avatar{width:30px;height:30px;border-radius:50%;color:#fff;display:grid;place-items:center;font-size:11px}.activity-item p{margin:3px 0 0;color:#64748b;font-size:12px}
@media (max-width: 1200px) { .kpi-grid,.bottom-grid { grid-template-columns:1fr; } }
@media (max-width: 860px) { .topbar { flex-direction:column; align-items:stretch; } .topbar-actions .ant-input-affix-wrapper, .project-progress { width:100%; min-width:0; } .weeklines{grid-template-columns:1fr} }
</style>
