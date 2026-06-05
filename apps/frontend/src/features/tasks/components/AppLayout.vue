<script setup lang="ts">
import AppSidebar from './AppSidebar.vue'

defineProps<{ taskCount?: number }>()
</script>

<template>
  <main class="dashboard-layout tm-app-shell">
    <AppSidebar :task-count="taskCount" />
    <section class="content">
      <slot />
    </section>
  </main>
</template>

<style scoped>
.dashboard-layout {
  display: grid;
  grid-template-columns: 240px 1fr;
}

.content {
  display: grid;
  gap: 16px;
  padding: 26px 30px;
}

@media (max-width: 1200px) {
  .dashboard-layout {
    grid-template-columns: 1fr;
  }

  :deep(.sidebar) {
    display: none;
  }
}
</style>
