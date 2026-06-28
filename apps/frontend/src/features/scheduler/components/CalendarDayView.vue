<script setup lang="ts">
import { computed, ref } from 'vue'
import CalendarEventCard from './CalendarEventCard.vue'
import ScheduledBlockDetailsDrawer from './ScheduledBlockDetailsDrawer.vue'
import type { ScheduledBlock, UpdateScheduledBlockPayload } from '../types'
import { HOUR_MARKS, layoutBlocksForDay, type ScheduledBlockLayout } from '../utils/calendarDates'

const props = defineProps<{ date: Date; blocks: ScheduledBlock[]; savingBlockIds: Set<string> }>()
const emit = defineEmits<{
  complete: [blockId: string]
  reschedule: [blockId: string, payload: UpdateScheduledBlockPayload]
}>()
const hourHeight = 72
const selectedBlock = ref<ScheduledBlock | null>(null)
const detailOpen = ref(false)
const laidOutBlocks = computed(() => layoutBlocksForDay(props.blocks, props.date))

function openBlockDetails(block: ScheduledBlock) {
  selectedBlock.value = block
  detailOpen.value = true
}

function forwardReschedule(blockId: string, payload: UpdateScheduledBlockPayload) {
  emit('reschedule', blockId, payload)
}

function blockStyle(layout: ScheduledBlockLayout) {
  const gutter = 6
  return {
    top: `${(layout.topMinutes / 60) * hourHeight}px`,
    height: `${(layout.durationMinutes / 60) * hourHeight}px`,
    left: `calc(68px + ((100% - 78px) * ${layout.laneIndex}) / ${layout.laneCount} + ${gutter / 2}px)`,
    right: 'auto',
    width: `calc((100% - 78px) / ${layout.laneCount} - ${gutter}px)`,
  }
}
</script>

<template>
  <a-card class="calendar-grid-card tm-card-surface">
    <div class="day-header">
      {{ date.toLocaleDateString(undefined, { weekday: 'long', month: 'short', day: 'numeric' }) }}
    </div>
    <div class="time-grid" :style="{ height: `${24 * hourHeight}px` }">
      <div
        v-for="hour in HOUR_MARKS"
        :key="hour"
        class="hour-row"
        :style="{ top: `${hour * hourHeight}px`, height: `${hourHeight}px` }"
      >
        <span>{{ hour.toString().padStart(2, '0') }}:00</span>
      </div>
      <div
        v-for="layout in laidOutBlocks"
        :key="layout.block.id"
        class="event-position"
        :style="blockStyle(layout)"
      >
        <CalendarEventCard :block="layout.block" @select="openBlockDetails" />
      </div>
    </div>

    <ScheduledBlockDetailsDrawer
      v-model:open="detailOpen"
      :block="selectedBlock"
      :saving="selectedBlock ? savingBlockIds.has(selectedBlock.id) : false"
      @complete="emit('complete', $event)"
      @reschedule="forwardReschedule"
    />
  </a-card>
</template>

<style scoped>
.day-header {
  border-bottom: 1px solid #e2e8f0;
  font-weight: 700;
  padding: 0 0 12px 68px;
}
.time-grid {
  min-height: 720px;
  position: relative;
}
.hour-row {
  border-top: 1px solid #e2e8f0;
  left: 0;
  position: absolute;
  right: 0;
}
.hour-row span {
  color: #64748b;
  display: block;
  font-size: 12px;
  width: 56px;
}
.event-position {
  left: 68px;
  min-height: 44px;
  position: absolute;
  right: 10px;
}
</style>
