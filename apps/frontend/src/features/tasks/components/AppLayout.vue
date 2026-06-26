<script setup lang="ts">
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { MenuOutlined } from '@ant-design/icons-vue'

import AppSidebar from './AppSidebar.vue'

defineProps<{ taskCount?: number }>()

const route = useRoute()
const mobileMenuOpen = ref(false)
const sidebarCollapsed = ref(false)
const sidebarWidth = ref(240)
const isResizingSidebar = ref(false)

const SIDEBAR_MIN_WIDTH = 200
const SIDEBAR_MAX_WIDTH = 380
const SIDEBAR_COLLAPSED_WIDTH = 72

const desktopSidebarWidth = computed(() =>
  sidebarCollapsed.value ? SIDEBAR_COLLAPSED_WIDTH : sidebarWidth.value,
)

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

function toggleSidebarCollapse() {
  sidebarCollapsed.value = !sidebarCollapsed.value
}

function clampSidebarWidth(width: number) {
  return Math.min(SIDEBAR_MAX_WIDTH, Math.max(SIDEBAR_MIN_WIDTH, Math.round(width)))
}

function stopSidebarResize() {
  if (!isResizingSidebar.value) return

  isResizingSidebar.value = false
  document.body.style.cursor = ''
  document.body.style.userSelect = ''
  window.removeEventListener('pointermove', resizeSidebar)
  window.removeEventListener('pointerup', stopSidebarResize)
}

function resizeSidebar(event: PointerEvent) {
  sidebarWidth.value = clampSidebarWidth(event.clientX)
}

function startSidebarResize(event: PointerEvent) {
  if (sidebarCollapsed.value) {
    sidebarCollapsed.value = false
  }

  isResizingSidebar.value = true
  sidebarWidth.value = clampSidebarWidth(event.clientX)
  document.body.style.cursor = 'col-resize'
  document.body.style.userSelect = 'none'
  window.addEventListener('pointermove', resizeSidebar)
  window.addEventListener('pointerup', stopSidebarResize)
}

watch(
  () => route.fullPath,
  () => closeMobileMenu(),
)

onBeforeUnmount(() => stopSidebarResize())
</script>

<template>
  <main
    class="dashboard-layout tm-app-shell"
    :class="{ 'is-sidebar-resizing': isResizingSidebar }"
    :style="{ '--desktop-sidebar-width': `${desktopSidebarWidth}px` }"
  >
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

    <div class="desktop-sidebar-frame">
      <AppSidebar
        :task-count="taskCount"
        :collapsed="sidebarCollapsed"
        @toggle-collapse="toggleSidebarCollapse"
      />
      <button
        v-if="!sidebarCollapsed"
        class="sidebar-resize-handle"
        type="button"
        aria-label="Resize workspace navigation sidebar"
        title="Drag to resize sidebar"
        @pointerdown="startSidebarResize"
      />
    </div>

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
  grid-template-columns: var(--desktop-sidebar-width, 240px) minmax(0, 1fr);
  min-height: 100vh;
  overflow: hidden;
  background: var(--tm-bg);
}

.desktop-sidebar-frame {
  position: relative;
  min-width: 0;
}

.sidebar-resize-handle {
  position: absolute;
  top: 0;
  right: -4px;
  bottom: 0;
  z-index: 15;
  width: 8px;
  padding: 0;
  cursor: col-resize;
  background: transparent;
  border: 0;
}

.sidebar-resize-handle::after {
  position: absolute;
  top: 0;
  bottom: 0;
  left: 3px;
  width: 2px;
  content: '';
  background: transparent;
  transition: background 160ms ease;
}

.sidebar-resize-handle:hover::after,
.sidebar-resize-handle:focus-visible::after,
.is-sidebar-resizing .sidebar-resize-handle::after {
  background: var(--tm-primary);
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

  .desktop-sidebar-frame {
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
