<script setup lang="ts">
import { computed } from 'vue'
import { getTaskStatusPresentation } from '../constants/taskPresentation'
import type { TaskStatus } from '../types'

const props = defineProps<{
  status: TaskStatus
}>()

const presentation = computed(() => getTaskStatusPresentation(props.status))
</script>

<template>
  <a-tag :color="presentation.color" class="task-status-chip">
    {{ presentation.label }}
  </a-tag>
</template>

<style scoped>
.task-status-chip {
  margin-inline-end: 0;
}
</style>
