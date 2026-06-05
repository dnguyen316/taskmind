<script setup lang="ts">
import { computed } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import { CheckSquareOutlined, FolderOutlined, TeamOutlined, CalendarOutlined, InboxOutlined, BarChartOutlined } from '@ant-design/icons-vue'

defineProps<{ taskCount?: number }>()
const route = useRoute()

const isDashboard = computed(() => route.path.startsWith('/dashboard'))
const isTasks = computed(() => route.path.startsWith('/tasks'))
const isProjects = computed(() => route.path.startsWith('/projects'))
const isTeam = computed(() => route.path.startsWith('/team'))
const isCalendar = computed(() => route.path.startsWith('/calendar'))
const isInbox = computed(() => route.path.startsWith('/inbox'))
const isReports = computed(() => route.path.startsWith('/reports'))
</script>

<template>
  <aside class="sidebar">
    <div class="brand">Taskmind <span>AI</span></div>
    <nav class="menu-group">
      <RouterLink to="/dashboard" class="menu-item" :class="{ active: isDashboard }"><CheckSquareOutlined />Dashboard</RouterLink>
      <RouterLink to="/tasks" class="menu-item" :class="{ active: isTasks }"><CheckSquareOutlined />Tasks <em>{{ taskCount ?? 0 }}</em></RouterLink>
      <RouterLink to="/projects" class="menu-item" :class="{ active: isProjects }"><FolderOutlined />Projects</RouterLink>
    </nav>
    <p class="group-title">Workspace</p>
    <nav class="menu-group">
      <RouterLink to="/team" class="menu-item" :class="{ active: isTeam }"><TeamOutlined />Team <span class="coming-soon">Coming M12</span></RouterLink>
      <RouterLink to="/calendar" class="menu-item" :class="{ active: isCalendar }"><CalendarOutlined />Calendar <span class="coming-soon">Coming M04</span></RouterLink>
      <RouterLink to="/inbox" class="menu-item" :class="{ active: isInbox }"><InboxOutlined />Inbox <span class="coming-soon">Coming M08</span></RouterLink>
      <RouterLink to="/reports" class="menu-item" :class="{ active: isReports }"><BarChartOutlined />Reports <span class="coming-soon">Coming M12</span></RouterLink>
    </nav>
    <div class="user-pill">
      <div class="avatar">AC</div>
      <div><strong>Alex Chen</strong><p>Acme · Product</p></div>
    </div>
  </aside>
</template>

<style scoped>
.sidebar { background:#fff; border-right:1px solid #e2e8f0; padding:24px 16px; display:flex; flex-direction:column; }
.brand { font-size:28px; font-weight:700; margin-bottom:20px; }
.brand span { font-size:12px; color:#2563eb; margin-left:8px; }
.menu-group { display:grid; gap:6px; margin-bottom:18px; }
.menu-item { display:flex; gap:10px; align-items:center; padding:10px 12px; border-radius:10px; color:#334155; }
.menu-item.active { background:#eaf1ff; color:#1d4ed8; font-weight:600; }
.menu-item em { margin-left:auto; font-style:normal; background:#e2e8f0; border-radius:999px; padding:0 8px; font-size:12px; }
.coming-soon { margin-left:auto; color:#64748b; background:#f1f5f9; border-radius:999px; padding:1px 7px; font-size:11px; white-space:nowrap; }
.group-title { text-transform:uppercase; color:#94a3b8; letter-spacing:.08em; font-size:12px; margin:6px 0; }
.user-pill { margin-top:auto; padding:10px; border-top:1px solid #e2e8f0; display:flex; gap:10px; align-items:center; }
.avatar { width:32px; height:32px; border-radius:50%; background:#1e40af; color:#fff; display:grid; place-items:center; font-size:12px; }
.user-pill p { margin:0; color:#64748b; font-size:12px; }
</style>
