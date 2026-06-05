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
  <aside class="sidebar tm-shell-sidebar">
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
.sidebar {
  display: flex;
  flex-direction: column;
  padding: 24px 16px;
  border-right: 1px solid var(--tm-border);
}

.brand {
  margin-bottom: 20px;
  font-size: 28px;
  font-weight: 700;
  color: var(--tm-text);
}

.brand span {
  margin-left: 8px;
  color: var(--tm-accent-blue);
  font-size: 12px;
}

.menu-group {
  display: grid;
  gap: 6px;
  margin-bottom: 18px;
}

.menu-item {
  display: flex;
  gap: 10px;
  align-items: center;
  padding: 10px 12px;
  color: var(--tm-text-muted);
  border-radius: calc(var(--tm-radius) - 4px);
}

.menu-item.active {
  color: var(--tm-accent-blue-strong);
  font-weight: 600;
  background: var(--tm-primary-soft);
}

.menu-item em,
.coming-soon {
  margin-left: auto;
  padding: 0 8px;
  color: var(--tm-text-muted);
  font-size: 12px;
  font-style: normal;
  white-space: nowrap;
  background: var(--tm-surface-muted);
  border-radius: 999px;
}

.coming-soon {
  padding: 1px 7px;
  font-size: 11px;
}

.group-title {
  margin: 6px 0;
  color: var(--tm-text-soft);
  font-size: 12px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.user-pill {
  display: flex;
  gap: 10px;
  align-items: center;
  margin-top: auto;
  padding: 10px;
  border-top: 1px solid var(--tm-border);
}

.avatar {
  display: grid;
  width: 32px;
  height: 32px;
  color: var(--tm-accent-contrast);
  font-size: 12px;
  background: var(--tm-accent-navy);
  border-radius: 50%;
  place-items: center;
}

.user-pill p {
  margin: 0;
  color: var(--tm-muted);
  font-size: 12px;
}
</style>
