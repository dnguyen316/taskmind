<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { MenuOutlined } from '@ant-design/icons-vue'

import AppSidebar from './AppSidebar.vue'

defineProps<{ taskCount?: number }>()

const route = useRoute()
const mobileMenuOpen = ref(false)

const currentPageTitle = computed(() => {
  if (route.path.startsWith('/dashboard')) return 'Dashboard'
  if (route.path.startsWith('/tasks/')) return 'Task detail'
  if (route.path.startsWith('/tasks')) return 'Tasks'
  if (route.name === 'project-task-detail') return 'Task detail'
  if (route.path.startsWith('/projects/')) return 'Project detail'
  if (route.path.startsWith('/projects')) return 'Projects'
  if (route.path.startsWith('/team')) return 'Team'
  if (route.path.startsWith('/calendar')) return 'Calendar'
  if (route.path.startsWith('/inbox')) return 'Inbox'
  if (route.path.startsWith('/reports')) return 'Reports'
  if (route.path.startsWith('/activity')) return 'Activity'

  return ''
})

function closeMobileMenu() {
  mobileMenuOpen.value = false
}

watch(
  () => route.fullPath,
  () => closeMobileMenu(),
)
</script>

<template>
  <main class="dashboard-layout tm-app-shell">
    <header class="mobile-topbar" aria-label="Authenticated workspace navigation">
      <div class="mobile-brand-group">
        <div class="mobile-brand">Taskmind <span>AI</span></div>
        <p v-if="currentPageTitle" class="mobile-page-title">{{ currentPageTitle }}</p>
      </div>
      <a-button
        class="mobile-menu-button"
        type="text"
        aria-label="Open workspace navigation menu"
        :aria-expanded="mobileMenuOpen"
        aria-controls="mobile-navigation-drawer"
        @click="mobileMenuOpen = true"
      >
        <MenuOutlined />
      </a-button>
    </header>

    <AppSidebar :task-count="taskCount" />

    <a-drawer
      id="mobile-navigation-drawer"
      v-model:open="mobileMenuOpen"
      class="mobile-navigation-drawer"
      placement="left"
      :closable="false"
      :width="300"
      title="Workspace navigation"
    >
      <AppSidebar :task-count="taskCount" mobile @navigate="closeMobileMenu" />
    </a-drawer>

    <section class="workspace-main" aria-label="Workspace page">
      <header class="desktop-topbar" aria-label="Page header">
        <div class="desktop-title-group">
          <h1 class="desktop-title">
            <slot name="title">{{ currentPageTitle }}</slot>
          </h1>
          <p class="desktop-subtitle">
            <slot name="subtitle" />
          </p>
        </div>
        <div class="desktop-topbar-actions">
          <slot name="headerActions" />
        </div>
      </header>

      <div class="content">
        <slot />
      </div>
    </section>
  </main>
</template>

<style scoped>
.dashboard-layout {
  display: grid;
  grid-template-columns: 240px minmax(0, 1fr);
  min-height: 100vh;
  overflow: hidden;
  background: var(--tm-bg);
}

.mobile-topbar {
  display: none;
}

:deep(.sidebar:not(.sidebar-mobile)) {
  position: sticky;
  top: 0;
  height: 100vh;
  overflow-y: auto;
}

.workspace-main {
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
  min-width: 0;
  min-height: 100vh;
  overflow: hidden;
}

.desktop-topbar {
  position: sticky;
  top: 0;
  z-index: 10;
  display: flex;
  gap: 20px;
  align-items: center;
  justify-content: space-between;
  min-height: 74px;
  padding: 16px 28px;
  background: color-mix(in srgb, var(--tm-surface) 94%, transparent);
  border-bottom: 1px solid var(--tm-border);
  backdrop-filter: blur(18px);
}

.desktop-title-group {
  min-width: 0;
}

.desktop-title {
  margin: 0;
  overflow: hidden;
  color: var(--tm-text);
  font-size: 24px;
  line-height: 1.2;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.desktop-subtitle {
  margin: 4px 0 0;
  overflow: hidden;
  color: var(--tm-text-muted);
  font-size: 13px;
  line-height: 1.4;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.desktop-subtitle:empty {
  display: none;
}

.desktop-topbar-actions {
  display: flex;
  flex: 0 1 auto;
  gap: 10px;
  align-items: center;
  justify-content: flex-end;
  min-width: 0;
}

.content {
  display: grid;
  gap: 16px;
  align-content: start;
  min-width: 0;
  overflow: auto;
  padding: 20px 28px 28px;
}

@media (max-width: 1200px) {
  .dashboard-layout {
    grid-template-columns: 1fr;
    overflow: visible;
  }

  .mobile-topbar {
    position: sticky;
    top: 0;
    z-index: 20;
    display: flex;
    align-items: center;
    justify-content: space-between;
    min-height: 64px;
    padding: 12px 20px;
    background: var(--tm-surface);
    border-bottom: 1px solid var(--tm-border);
  }

  .mobile-brand-group {
    min-width: 0;
  }

  .mobile-brand {
    color: var(--tm-text);
    font-size: 20px;
    font-weight: 700;
  }

  .mobile-brand span {
    margin-left: 6px;
    color: var(--tm-accent-blue);
    font-size: 11px;
  }

  .mobile-page-title {
    margin: 2px 0 0;
    overflow: hidden;
    color: var(--tm-text-muted);
    font-size: 13px;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .mobile-menu-button {
    display: inline-grid;
    width: 40px;
    height: 40px;
    color: var(--tm-text);
    place-items: center;
  }

  .workspace-main {
    min-height: calc(100vh - 64px);
    overflow: visible;
  }

  .desktop-topbar {
    display: none;
  }

  .content {
    overflow: visible;
    padding: 20px;
  }

  :deep(.sidebar:not(.sidebar-mobile)) {
    display: none;
  }
}

@media (max-width: 640px) {
  .content {
    padding: 16px;
  }
}

:global(.mobile-navigation-drawer .ant-drawer-body) {
  padding: 0;
}
</style>
