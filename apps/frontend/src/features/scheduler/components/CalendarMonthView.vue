<script setup lang="ts">
import { computed } from 'vue'
import CalendarEventCard from './CalendarEventCard.vue'
import type { ScheduledBlock, UpdateScheduledBlockPayload } from '../types'
import { blocksForDay, buildMonthGrid } from '../utils/calendarDates'

const props = defineProps<{ date: Date; blocks: ScheduledBlock[]; savingBlockIds: Set<string> }>()
const emit = defineEmits<{
  complete: [blockId: string]
  reschedule: [blockId: string, payload: UpdateScheduledBlockPayload]
}>()
const days = computed(() => buildMonthGrid(props.date))
</script>

<template>
  <a-card class="calendar-grid-card tm-card-surface month-card">
    <div class="weekday-row">
      <strong v-for="weekday in ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat']" :key="weekday">{{
        weekday
      }}</strong>
    </div>
    <div class="month-grid">
      <article
        v-for="day in days"
        :key="day.date.toISOString()"
        class="month-cell"
        :class="{ muted: !day.isCurrentMonth, today: day.isToday }"
      >
        <div class="day-number">{{ day.date.getDate() }}</div>
        <div class="month-events">
          <CalendarEventCard
            v-for="block in blocksForDay(blocks, day.date).slice(0, 3)"
            :key="block.id"
            compact
            :block="block"
            :saving="savingBlockIds.has(block.id)"
            @complete="emit('complete', $event)"
            @reschedule="(id, payload) => emit('reschedule', id, payload)"
          />
          <span v-if="blocksForDay(blocks, day.date).length > 3" class="overflow"
            >+{{ blocksForDay(blocks, day.date).length - 3 }} more</span
          >
        </div>
      </article>
    </div>
  </a-card>
</template>

<style scoped>
.weekday-row,
.month-grid {
  display: grid;
  grid-template-columns: repeat(7, minmax(120px, 1fr));
}
.weekday-row {
  color: #475569;
  text-align: center;
}
.month-grid {
  border-left: 1px solid #e2e8f0;
  border-top: 1px solid #e2e8f0;
  overflow-x: auto;
}
.month-cell {
  border-bottom: 1px solid #e2e8f0;
  border-right: 1px solid #e2e8f0;
  display: grid;
  gap: 6px;
  min-height: 150px;
  padding: 8px;
}
.month-cell.muted {
  background: #f8fafc;
  color: #94a3b8;
}
.month-cell.today .day-number {
  background: #2563eb;
  color: white;
}
.day-number {
  border-radius: 999px;
  display: grid;
  font-weight: 700;
  height: 28px;
  place-items: center;
  width: 28px;
}
.month-events {
  display: grid;
  gap: 5px;
  min-width: 0;
}
.overflow {
  color: #475569;
  font-size: 12px;
  font-weight: 700;
  padding-left: 4px;
}
@media (max-width: 800px) {
  .weekday-row,
  .month-grid {
    grid-template-columns: repeat(7, minmax(104px, 1fr));
  }
  .month-card {
    overflow-x: auto;
  }
}
</style>
