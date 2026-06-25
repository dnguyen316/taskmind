<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import AppLayout from '../../tasks/components/AppLayout.vue'
import CalendarDateNavigator from '../components/CalendarDateNavigator.vue'
import CalendarDayView from '../components/CalendarDayView.vue'
import CalendarMonthView from '../components/CalendarMonthView.vue'
import CalendarToolbar from '../components/CalendarToolbar.vue'
import CalendarViewSwitcher from '../components/CalendarViewSwitcher.vue'
import CalendarWeekView from '../components/CalendarWeekView.vue'
import RescheduleProposalList from '../components/RescheduleProposalList.vue'
import SchedulingPreferencesForm from '../components/SchedulingPreferencesForm.vue'
import { useScheduledBlocks } from '../composables/useScheduledBlocks'
import { useScheduleGeneration } from '../composables/useScheduleGeneration'
import { useSchedulingPreferences } from '../composables/useSchedulingPreferences'
import type {
  CalendarViewMode,
  RescheduleProposal,
  UpdateScheduledBlockPayload,
  UpdateSchedulingPreferencesPayload,
} from '../types'
import {
  addPeriods,
  blocksForDay,
  daysBetweenInclusive,
  formatVisibleRangeTitle,
  getVisibleRange,
  toSchedulerRangeParams,
} from '../utils/calendarDates'

const preferencesState = useSchedulingPreferences()
const blocksState = useScheduledBlocks()
const generationState = useScheduleGeneration()

const initialLoadComplete = ref(false)
const pageMessage = ref('')
const proposals = ref<RescheduleProposal[]>([])
const selectedDate = ref(new Date())
const viewMode = ref<CalendarViewMode>('week')

const visibleRange = computed(() => getVisibleRange(selectedDate.value, viewMode.value))
const rangeParams = computed(() => toSchedulerRangeParams(visibleRange.value))
const visibleTitle = computed(() => formatVisibleRangeTitle(selectedDate.value, viewMode.value))
const visibleDays = computed(() =>
  daysBetweenInclusive(visibleRange.value.from, visibleRange.value.to),
)
const visibleBlocks = computed(() => blocksState.sortedBlocks.value)
const dayBlocks = computed(() => blocksForDay(visibleBlocks.value, selectedDate.value))
const loading = computed(() => preferencesState.loading.value || blocksState.loading.value)
const scheduledCount = computed(
  () => blocksState.sortedBlocks.value.filter((block) => block.status === 'SCHEDULED').length,
)
const errorMessage = computed(
  () =>
    preferencesState.errorMessage.value ||
    blocksState.errorMessage.value ||
    generationState.errorMessage.value,
)
const hasEmptySchedule = computed(
  () => initialLoadComplete.value && !loading.value && !blocksState.hasBlocks.value,
)

watch([selectedDate, viewMode], () => {
  void blocksState.fetchBlocks(rangeParams.value)
})

onMounted(() => {
  void loadScheduler()
})

async function loadScheduler() {
  pageMessage.value = ''
  try {
    await Promise.all([
      preferencesState.fetchPreferences(),
      blocksState.fetchBlocks(rangeParams.value),
    ])
  } finally {
    initialLoadComplete.value = true
  }
}

async function savePreferences(payload: UpdateSchedulingPreferencesPayload) {
  await preferencesState.savePreferences(payload)
  pageMessage.value = preferencesState.successMessage.value
}

async function generateSchedule() {
  pageMessage.value = ''
  const result = await generationState.generate(rangeParams.value)
  blocksState.mergeBlocks(result.blocks)
  proposals.value = result.proposals
  pageMessage.value = result.blocks.length
    ? `Generated ${result.blocks.length} scheduled block${result.blocks.length === 1 ? '' : 's'}.`
    : 'No new blocks were generated for the current task window.'
}

async function completeBlock(blockId: string) {
  await blocksState.completeBlock(blockId)
  pageMessage.value = 'Scheduled block completed.'
}

async function rescheduleBlock(blockId: string, payload: UpdateScheduledBlockPayload) {
  await blocksState.saveBlock(blockId, payload)
  pageMessage.value = 'Scheduled block updated.'
}

function movePeriod(direction: 1 | -1) {
  selectedDate.value = addPeriods(selectedDate.value, viewMode.value, direction)
}
</script>

<template>
  <AppLayout>
    <section class="calendar-page">
      <CalendarToolbar
        :loading="loading"
        :generating="generationState.generating.value"
        :missed-count="blocksState.missedBlocks.value.length"
        :scheduled-count="scheduledCount"
        @refresh="loadScheduler"
        @generate="generateSchedule"
      />

      <a-alert v-if="errorMessage" type="error" show-icon :message="errorMessage" />
      <a-alert v-else-if="pageMessage" type="success" show-icon :message="pageMessage" />

      <a-alert
        v-if="blocksState.missedBlocks.value.length"
        type="warning"
        show-icon
        message="Missed scheduled work needs attention"
        :description="`${blocksState.missedBlocks.value.length} block${blocksState.missedBlocks.value.length === 1 ? '' : 's'} ended without completion. Generate a schedule to refresh reschedule proposals.`"
      />

      <div class="scheduler-grid">
        <div class="main-column">
          <a-card class="calendar-shell tm-card-surface">
            <div class="calendar-controls">
              <CalendarDateNavigator
                :title="visibleTitle"
                :loading="blocksState.loading.value"
                @today="selectedDate = new Date()"
                @previous="movePeriod(-1)"
                @next="movePeriod(1)"
              />
              <CalendarViewSwitcher :view-mode="viewMode" @change="viewMode = $event" />
            </div>
          </a-card>

          <CalendarDayView
            v-if="viewMode === 'day'"
            :date="selectedDate"
            :blocks="dayBlocks"
            :saving-block-ids="blocksState.savingBlockIds.value"
            @complete="completeBlock"
            @reschedule="rescheduleBlock"
          />
          <CalendarWeekView
            v-else-if="viewMode === 'week'"
            :days="visibleDays"
            :blocks="visibleBlocks"
            :saving-block-ids="blocksState.savingBlockIds.value"
            @complete="completeBlock"
            @reschedule="rescheduleBlock"
          />
          <CalendarMonthView
            v-else
            :date="selectedDate"
            :blocks="visibleBlocks"
            :saving-block-ids="blocksState.savingBlockIds.value"
            @complete="completeBlock"
            @reschedule="rescheduleBlock"
          />

          <a-card v-if="hasEmptySchedule" class="empty-state tm-card-surface">
            <a-empty description="Your calendar has no scheduler-owned blocks yet.">
              <a-button
                type="primary"
                :loading="generationState.generating.value"
                @click="generateSchedule"
                >Generate my first schedule</a-button
              >
            </a-empty>
          </a-card>
        </div>

        <aside class="side-column">
          <SchedulingPreferencesForm
            :preferences="preferencesState.preferences.value"
            :loading="preferencesState.loading.value"
            :saving="preferencesState.saving.value"
            @submit="savePreferences"
          />
          <RescheduleProposalList :proposals="proposals" />
        </aside>
      </div>
    </section>
  </AppLayout>
</template>

<style scoped>
.calendar-page {
  display: grid;
  gap: 16px;
}
.scheduler-grid {
  align-items: start;
  display: grid;
  gap: 16px;
  grid-template-columns: minmax(0, 1.4fr) minmax(340px, 0.6fr);
}
.main-column,
.side-column {
  display: grid;
  gap: 16px;
}
.calendar-controls {
  align-items: center;
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  justify-content: space-between;
}
.empty-state {
  border: 1px dashed #c4b5fd;
}
@media (max-width: 1100px) {
  .scheduler-grid {
    grid-template-columns: 1fr;
  }
}
@media (max-width: 700px) {
  .calendar-controls {
    align-items: stretch;
    flex-direction: column;
  }
}
</style>
