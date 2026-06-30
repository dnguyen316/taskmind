<script setup lang="ts">
import { computed } from 'vue'
import {
  CalendarOutlined,
  LeftOutlined,
  ReloadOutlined,
  RightOutlined,
} from '@ant-design/icons-vue'
import type { CalendarViewMode } from '../types'

const props = defineProps<{
  loading?: boolean
  generating?: boolean
  missedCount: number
  scheduledCount: number
  viewMode: CalendarViewMode
  periodLabel: string
  selectedDate: Date | string
}>()

const emit = defineEmits<{
  refresh: []
  generate: []
  'update:viewMode': [mode: CalendarViewMode]
  today: []
  previous: []
  next: []
}>()

const viewOptions: Array<{ label: string; value: CalendarViewMode }> = [
  { label: 'Day', value: 'day' },
  { label: 'Week', value: 'week' },
  { label: 'Month', value: 'month' },
]

const selectedDateLabel = computed(() => {
  const date =
    props.selectedDate instanceof Date ? props.selectedDate : new Date(props.selectedDate)

  return Number.isNaN(date.getTime()) ? props.periodLabel : date.toLocaleDateString()
})

function updateViewMode(value: string | number) {
  emit('update:viewMode', value as CalendarViewMode)
}
</script>

<template>
  <a-card class="calendar-toolbar tm-card-surface">
    <div class="toolbar-copy">
      <div class="icon"><CalendarOutlined /></div>
      <div>
        <a-tag color="purple">Preview</a-tag>
        <h1>Calendar</h1>
        <p :title="`Selected date: ${selectedDateLabel}`">{{ props.periodLabel }}</p>
      </div>
    </div>

    <div class="toolbar-actions">
      <a-segmented
        :value="props.viewMode"
        :options="viewOptions"
        class="view-mode-control"
        @change="updateViewMode"
      />
      <a-button-group class="period-controls">
        <a-button aria-label="Previous period" @click="emit('previous')">
          <template #icon><LeftOutlined /></template>
          Previous
        </a-button>
        <a-button @click="emit('today')">Today</a-button>
        <a-button aria-label="Next period" @click="emit('next')">
          Next
          <template #icon><RightOutlined /></template>
        </a-button>
      </a-button-group>
      <a-statistic title="Scheduled blocks" :value="props.scheduledCount" />
      <a-statistic title="Missed" :value="props.missedCount" />
      <a-button :loading="props.loading" @click="emit('refresh')">
        <template #icon><ReloadOutlined /></template>
        Refresh
      </a-button>
      <a-button type="primary" :loading="props.generating" @click="emit('generate')"
        >Generate schedule</a-button
      >
    </div>
  </a-card>
</template>

<style scoped>
.calendar-toolbar {
  border: 1px solid #ddd6fe;
  border-radius: 18px;
}

.calendar-toolbar :deep(.ant-card-body) {
  align-items: center;
  display: flex;
  gap: 24px;
  justify-content: space-between;
}

.toolbar-copy {
  align-items: center;
  display: flex;
  gap: 16px;
}

.icon {
  background: #f5f3ff;
  border-radius: 16px;
  color: #7c3aed;
  display: grid;
  flex: 0 0 auto;
  font-size: 26px;
  height: 52px;
  place-items: center;
  width: 52px;
}

h1 {
  margin: 8px 0 6px;
}

p {
  color: #475569;
  margin: 0;
  max-width: 680px;
}

.toolbar-actions {
  align-items: center;
  display: flex;
  flex-wrap: wrap;
  gap: 14px;
  justify-content: flex-end;
}

.toolbar-actions :deep(.ant-statistic) {
  min-width: 110px;
}

.view-mode-control {
  min-height: 40px;
}

.period-controls {
  white-space: nowrap;
}

@media (max-width: 900px) {
  .calendar-toolbar :deep(.ant-card-body),
  .toolbar-copy,
  .toolbar-actions {
    align-items: stretch;
    flex-direction: column;
  }

  .toolbar-actions {
    justify-content: flex-start;
  }
}
</style>
