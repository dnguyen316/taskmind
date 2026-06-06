<script setup lang="ts">
import { CalendarOutlined, ReloadOutlined } from '@ant-design/icons-vue'

const props = defineProps<{
  loading?: boolean
  generating?: boolean
  missedCount: number
  scheduledCount: number
}>()

const emit = defineEmits<{
  refresh: []
  generate: []
}>()
</script>

<template>
  <a-card class="calendar-toolbar tm-card-surface">
    <div class="toolbar-copy">
      <div class="icon"><CalendarOutlined /></div>
      <div>
        <a-tag color="purple">M04 Scheduler</a-tag>
        <h1>Calendar</h1>
        <p>
          Generate task blocks from Core scheduling preferences, track missed work, and review
          reschedule proposals.
        </p>
      </div>
    </div>

    <div class="toolbar-actions">
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
