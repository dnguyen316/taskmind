<script setup lang="ts">
import CalendarEventCard from './CalendarEventCard.vue'
import type { ScheduledBlock, UpdateScheduledBlockPayload } from '../types'
import {
  blockDurationMinutes,
  blocksForDay,
  HOUR_MARKS,
  minuteOffset,
} from '../utils/calendarDates'

defineProps<{ days: Date[]; blocks: ScheduledBlock[]; savingBlockIds: Set<string> }>()
const emit = defineEmits<{
  complete: [blockId: string]
  reschedule: [blockId: string, payload: UpdateScheduledBlockPayload]
}>()
const hourHeight = 72
</script>

<template>
  <a-card class="calendar-grid-card tm-card-surface week-card">
    <div class="week-scroll">
      <div
        class="week-header"
        :style="{ gridTemplateColumns: `64px repeat(${days.length}, minmax(140px, 1fr))` }"
      >
        <span></span>
        <strong v-for="day in days" :key="day.toISOString()">{{
          day.toLocaleDateString(undefined, { weekday: 'short', day: 'numeric' })
        }}</strong>
      </div>
      <div
        class="week-grid"
        :style="{
          height: `${24 * hourHeight}px`,
          gridTemplateColumns: `64px repeat(${days.length}, minmax(140px, 1fr))`,
        }"
      >
        <template v-for="hour in HOUR_MARKS" :key="hour">
          <div class="hour-label" :style="{ top: `${hour * hourHeight}px` }">
            {{ hour.toString().padStart(2, '0') }}:00
          </div>
          <div class="hour-line" :style="{ top: `${hour * hourHeight}px` }"></div>
        </template>
        <div
          v-for="(day, index) in days"
          :key="day.toISOString()"
          class="day-column"
          :style="{ gridColumn: index + 2 }"
        >
          <div
            v-for="block in blocksForDay(blocks, day)"
            :key="block.id"
            class="event-position"
            :style="{
              top: `${(minuteOffset(block.startsAt) / 60) * hourHeight}px`,
              height: `${(blockDurationMinutes(block) / 60) * hourHeight}px`,
            }"
          >
            <CalendarEventCard
              compact
              :block="block"
              :saving="savingBlockIds.has(block.id)"
              @complete="emit('complete', $event)"
              @reschedule="(id, payload) => emit('reschedule', id, payload)"
            />
          </div>
        </div>
      </div>
    </div>
  </a-card>
</template>

<style scoped>
.week-scroll {
  overflow-x: auto;
}
.week-header,
.week-grid {
  display: grid;
  min-width: 1040px;
}
.week-header {
  border-bottom: 1px solid #e2e8f0;
  color: #334155;
  padding-bottom: 10px;
  text-align: center;
}
.week-grid {
  min-height: 720px;
  position: relative;
}
.hour-label {
  color: #64748b;
  font-size: 12px;
  grid-column: 1;
  position: absolute;
}
.hour-line {
  border-top: 1px solid #e2e8f0;
  left: 64px;
  position: absolute;
  right: 0;
}
.day-column {
  border-left: 1px solid #e2e8f0;
  min-width: 0;
  position: relative;
}
.event-position {
  left: 6px;
  min-height: 36px;
  position: absolute;
  right: 6px;
}
</style>
