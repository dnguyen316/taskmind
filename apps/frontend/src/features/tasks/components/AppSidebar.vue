<script setup lang="ts">
import { computed } from 'vue'
import { storeToRefs } from 'pinia'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import {
  AuditOutlined,
  BarChartOutlined,
  CalendarOutlined,
  CheckSquareOutlined,
  FolderOutlined,
  InboxOutlined,
  LogoutOutlined,
  TeamOutlined,
} from '@ant-design/icons-vue'

import { useAuthStore } from '../../../stores/auth'

defineProps<{ taskCount?: number; mobile?: boolean }>()
const emit = defineEmits<{ navigate: [] }>()
const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const { currentUser } = storeToRefs(authStore)

const isDashboard = computed(() => route.path.startsWith('/dashboard'))
const isTasks = computed(() => route.path.startsWith('/tasks'))
const isProjects = computed(() => route.path.startsWith('/projects'))
const isTeam = computed(() => route.path.startsWith('/team'))
const isCalendar = computed(() => route.path.startsWith('/calendar'))
const isInbox = computed(() => route.path.startsWith('/inbox'))
const isReports = computed(() => route.path.startsWith('/reports'))
const isActivity = computed(() => route.path.startsWith('/activity'))
const userInitials = computed(() => initials(authStore.currentUserDisplayName))
const userEmail = computed(() => currentUser.value?.email ?? 'Signed in')

async function signOut() {
  await authStore.logout()
  emit('navigate')
  await router.push({ name: 'login' })
}

function initials(value: string) {
  const parts = value.trim().split(/\s+/).filter(Boolean)

  if (parts.length === 0) {
    return 'TM'
  }

  return parts
    .slice(0, 2)
    .map((part) => part[0]?.toUpperCase() ?? '')
    .join('')
}
</script>

<template>
  <aside class="sidebar tm-shell-sidebar" :class="{ 'sidebar-mobile': mobile }">
    <div class="brand">Taskmind <span>AI</span></div>
    <nav class="menu-group">
      <RouterLink
        to="/dashboard"
        class="menu-item"
        :class="{ active: isDashboard }"
        @click="emit('navigate')"
        ><CheckSquareOutlined />Dashboard</RouterLink
      >
      <RouterLink
        to="/tasks"
        class="menu-item"
        :class="{ active: isTasks }"
        @click="emit('navigate')"
        ><CheckSquareOutlined />Tasks <em>{{ taskCount ?? 0 }}</em></RouterLink
      >
      <RouterLink
        to="/projects"
        class="menu-item"
        :class="{ active: isProjects }"
        @click="emit('navigate')"
        ><FolderOutlined />Projects</RouterLink
      >
    </nav>
    <p class="group-title">Workspace</p>
    <nav class="menu-group">
      <RouterLink to="/team" class="menu-item" :class="{ active: isTeam }" @click="emit('navigate')"
        ><TeamOutlined />Team <span class="coming-soon">Coming M12</span></RouterLink
      >
      <RouterLink
        to="/calendar"
        class="menu-item"
        :class="{ active: isCalendar }"
        @click="emit('navigate')"
        ><CalendarOutlined />Calendar <span class="coming-soon">Coming M04</span></RouterLink
      >
      <RouterLink
        to="/inbox"
        class="menu-item"
        :class="{ active: isInbox }"
        @click="emit('navigate')"
        ><InboxOutlined />Inbox <span class="coming-soon">Coming M08</span></RouterLink
      >
      <RouterLink
        to="/reports"
        class="menu-item"
        :class="{ active: isReports }"
        @click="emit('navigate')"
        ><BarChartOutlined />Reports <span class="coming-soon">Coming M12</span></RouterLink
      >
      <RouterLink
        to="/activity"
        class="menu-item"
        :class="{ active: isActivity }"
        @click="emit('navigate')"
        ><AuditOutlined />Activity</RouterLink
      >
    </nav>
    <div class="user-pill">
      <div class="avatar">{{ userInitials }}</div>
      <div class="user-meta">
        <strong>{{ authStore.currentUserDisplayName }}</strong>
        <p>{{ userEmail }}</p>
      </div>
      <a-button
        class="logout-button"
        type="text"
        title="Log out"
        aria-label="Log out"
        @click="signOut"
        ><LogoutOutlined
      /></a-button>
    </div>
  </aside>
</template>

<style scoped>
.sidebar {
  display: flex;
  flex-direction: column;
  padding: 24px 16px;
  min-height: 100vh;
  border-right: 1px solid var(--tm-border);
}

.sidebar-mobile {
  min-height: 100%;
  border-right: 0;
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

.user-meta {
  min-width: 0;
}

.user-meta strong,
.user-meta p {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.user-pill p {
  margin: 0;
  color: var(--tm-muted);
  font-size: 12px;
}

.logout-button {
  margin-left: auto;
  color: var(--tm-text-muted);
}
</style>
