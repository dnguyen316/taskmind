<script setup lang="ts">
import type { CalendarViewMode } from '../types'

defineProps<{ viewMode: CalendarViewMode }>()
const emit = defineEmits<{ change: [mode: CalendarViewMode] }>()

function handleChange(value: string | number) {
  emit('change', value as CalendarViewMode)
}

const options: Array<{ label: string; value: CalendarViewMode }> = [
  { label: 'Day', value: 'day' },
  { label: 'Week', value: 'week' },
  { label: 'Month', value: 'month' },
]
</script>

<template>
  <a-segmented
    :value="viewMode"
    :options="options"
    class="calendar-view-switcher"
    @change="handleChange"
  />
</template>

<style scoped>
.calendar-view-switcher {
  min-height: 40px;
}
</style>
