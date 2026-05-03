<script setup lang="ts">
import AppErrorBoundary from './components/AppErrorBoundary.vue'
</script>

<template>
  <AppErrorBoundary>
    <header class="app-nav">
      <router-link :to="{ name: 'projects-dashboard' }">Projects</router-link>
      <router-link :to="{ name: 'tasks-dashboard' }">Tasks</router-link>
    </header>
    <router-view />
  </AppErrorBoundary>
</template>

<style scoped>
.app-nav {
  max-width: 1100px;
  margin: 20px auto 0;
  padding: 0 20px;
  display: flex;
  gap: 14px;
}

.app-nav a {
  color: #0369a1;
  font-weight: 600;
  text-decoration: none;
}

.app-nav a.router-link-active {
  color: #0f172a;
  text-decoration: underline;
}
</style>
