<script setup lang="ts">
import { computed } from 'vue'
import type { ScheduledBlock } from '../types'
import { compactTimeRange, durationLabel, statusLabel } from '../utils/scheduledBlockDisplay'

const props = defineProps<{ block: ScheduledBlock; compact?: boolean }>()
const emit = defineEmits<{ select: [block: ScheduledBlock] }>()

const cardClass = computed(() => `status-${props.block.status.toLowerCase()}`)
const isCompleted = computed(() => props.block.status === 'COMPLETED')
const isMissed = computed(() => props.block.status === 'MISSED')
</script>

<template>
  <button
    class="calendar-event-card"
    :class="[cardClass, { compact, completed: isCompleted, missed: isMissed }]"
    @click="emit('select', block)"
  >
    <span class="event-time">{{ compactTimeRange(block) }}</span>
    <strong>Task {{ block.taskId.slice(0, 8) }}</strong>
    <span v-if="!compact" class="event-meta"
      >{{ statusLabel(block.status) }} · {{ durationLabel(block) }}</span
    >
  </button>
</template>

<style scoped>
.calendar-event-card {
  background: #eff6ff;
  border: 1px solid #bfdbfe;
  border-left: 4px solid #2563eb;
  border-radius: 10px;
  color: #1e3a8a;
  cursor: pointer;
  display: grid;
  gap: 2px;
  min-height: 44px;
  padding: 7px 9px;
  text-align: left;
  width: 100%;
}
.calendar-event-card.status-completed {
  background: #ecfdf5;
  border-color: #86efac;
  border-left-color: #16a34a;
  color: #166534;
}
.calendar-event-card.status-missed {
  background: #fffbeb;
  border-color: #fcd34d;
  border-left-color: #f59e0b;
  color: #92400e;
}
.calendar-event-card.status-cancelled {
  background: #f8fafc;
  border-color: #cbd5e1;
  border-left-color: #64748b;
  color: #475569;
}
.calendar-event-card.compact {
  font-size: 12px;
  min-height: 34px;
  overflow: hidden;
  padding: 5px 7px;
}
.calendar-event-card.completed {
  text-decoration-color: rgba(22, 101, 52, 0.55);
  text-decoration-line: line-through;
}
.calendar-event-card.missed {
  box-shadow: inset 0 0 0 1px rgba(245, 158, 11, 0.2);
}
.event-time,
.event-meta {
  color: inherit;
  font-size: 12px;
  opacity: 0.78;
}
</style>
