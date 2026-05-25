<script setup lang="ts">
import AppErrorBoundary from './components/AppErrorBoundary.vue'
import AiAssistantWidget from './components/AiAssistantWidget.vue'
</script>

<template>
  <AppErrorBoundary>
    <router-view />
    <AiAssistantWidget />
  </AppErrorBoundary>
</template>
