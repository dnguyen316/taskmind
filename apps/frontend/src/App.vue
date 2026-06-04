<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import AppErrorBoundary from './components/AppErrorBoundary.vue'
import AiAssistantWidget from './components/AiAssistantWidget.vue'
import { useTheme } from './composables/useTheme'

useTheme()

const route = useRoute()
const showAssistant = computed(() => !['landing', 'login', 'signup'].includes(String(route.name)))
</script>

<template>
  <AppErrorBoundary>
    <router-view />
    <AiAssistantWidget v-if="showAssistant" />
  </AppErrorBoundary>
</template>
