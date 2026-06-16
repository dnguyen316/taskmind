<script setup lang="ts">
import { computed } from 'vue'
import { storeToRefs } from 'pinia'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import {
  AppstoreOutlined,
  AuditOutlined,
  BarChartOutlined,
  CalendarOutlined,
  FolderOutlined,
  InboxOutlined,
  LogoutOutlined,
  TeamOutlined,
  UnorderedListOutlined,
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
        ><AppstoreOutlined />Dashboard</RouterLink
      >
      <RouterLink
        to="/tasks"
        class="menu-item"
        :class="{ active: isTasks }"
        @click="emit('navigate')"
        ><UnorderedListOutlined />Tasks <em>{{ taskCount ?? 0 }}</em></RouterLink
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
      <RouterLink
        to="/team"
        class="menu-item"
        :class="{ active: isTeam }"
        @click="emit('navigate')"
        title="Team directory is planned and currently shows availability details"
        ><TeamOutlined />Team <span class="nav-badge">Soon</span></RouterLink
      >
      <RouterLink
        to="/calendar"
        class="menu-item"
        :class="{ active: isCalendar }"
        @click="emit('navigate')"
        title="Calendar scheduling is available as an early preview"
        ><CalendarOutlined />Calendar <span class="nav-badge">Preview</span></RouterLink
      >
      <RouterLink
        to="/inbox"
        class="menu-item"
        :class="{ active: isInbox }"
        @click="emit('navigate')"
        title="Inbox capture AI tools are available as a beta"
        ><InboxOutlined />Inbox <span class="nav-badge">Beta</span></RouterLink
      >
      <RouterLink
        to="/reports"
        class="menu-item"
        :class="{ active: isReports }"
        @click="emit('navigate')"
        title="Reports are planned and currently show availability details"
        ><BarChartOutlined />Reports <span class="nav-badge">Soon</span></RouterLink
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
  box-shadow: 10px 0 30px rgba(15, 23, 42, 0.035);
}

.sidebar-mobile {
  min-height: 100%;
  border-right: 0;
}

.brand {
  margin-bottom: 22px;
  color: var(--tm-text);
  font-size: 28px;
  font-weight: 800;
  letter-spacing: -0.04em;
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
  font-size: 14px;
  font-weight: 520;
  letter-spacing: -0.01em;
  text-decoration: none;
  border-radius: calc(var(--tm-radius) - 4px);
  transition:
    color 160ms ease,
    background 160ms ease,
    transform 160ms ease;
}

.menu-item:hover,
.menu-item:focus-visible {
  color: var(--tm-primary);
  text-decoration: none;
  background: var(--tm-hover);
}

.menu-item.active {
  color: var(--tm-primary);
  font-weight: 700;
  background: linear-gradient(135deg, rgba(79, 70, 229, 0.14), rgba(14, 165, 233, 0.12));
}

.menu-item :deep(.anticon) {
  color: currentColor;
  font-size: 16px;
}

.menu-item em,
.nav-badge {
  margin-left: auto;
  padding: 0 8px;
  color: var(--tm-text-muted);
  font-size: 12px;
  font-style: normal;
  white-space: nowrap;
  background: var(--tm-surface-muted);
  border-radius: 999px;
}

.nav-badge {
  padding: 1px 7px;
  font-size: 11px;
}

.group-title {
  margin: 6px 0;
  color: var(--tm-text-soft);
  font-size: 11px;
  font-weight: 700;
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
  flex: 0 0 36px;
  width: 36px;
  height: 36px;
  color: var(--tm-accent-contrast);
  font-size: 12px;
  font-weight: 800;
  background: var(--tm-ai-grad);
  border-radius: 999px;
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
