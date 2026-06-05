<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { CheckSquareOutlined, FolderOutlined, TeamOutlined, CalendarOutlined, InboxOutlined, BarChartOutlined } from '@ant-design/icons-vue'

const props = defineProps<{ taskCount?: number }>()
const route = useRoute()

const isDashboard = computed(() => route.path.startsWith('/dashboard'))
const isTasks = computed(() => route.path.startsWith('/tasks'))
const isProjects = computed(() => route.path.startsWith('/projects'))
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
      <a class="menu-item"><TeamOutlined />Team</a>
      <a class="menu-item"><CalendarOutlined />Calendar</a>
      <a class="menu-item"><InboxOutlined />Inbox</a>
      <a class="menu-item"><BarChartOutlined />Reports</a>
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

.menu-item em {
  margin-left: auto;
  padding: 0 8px;
  font-size: 12px;
  font-style: normal;
  color: var(--tm-text-muted);
  background: var(--tm-surface-muted);
  border-radius: 999px;
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
