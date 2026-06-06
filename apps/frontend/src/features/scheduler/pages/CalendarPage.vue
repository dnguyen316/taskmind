<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import AppLayout from '../../tasks/components/AppLayout.vue'
import CalendarToolbar from '../components/CalendarToolbar.vue'
import RescheduleProposalList from '../components/RescheduleProposalList.vue'
import ScheduledBlockList from '../components/ScheduledBlockList.vue'
import SchedulingPreferencesForm from '../components/SchedulingPreferencesForm.vue'
import { useScheduledBlocks } from '../composables/useScheduledBlocks'
import { useScheduleGeneration } from '../composables/useScheduleGeneration'
import { useSchedulingPreferences } from '../composables/useSchedulingPreferences'
import type {
  RescheduleProposal,
  UpdateScheduledBlockPayload,
  UpdateSchedulingPreferencesPayload,
} from '../types'

const preferencesState = useSchedulingPreferences()
const blocksState = useScheduledBlocks()
const generationState = useScheduleGeneration()

const initialLoadComplete = ref(false)
const pageMessage = ref('')
const proposals = ref<RescheduleProposal[]>([])

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

onMounted(() => {
  void loadScheduler()
})

async function loadScheduler() {
  pageMessage.value = ''
  try {
    await Promise.all([preferencesState.fetchPreferences(), blocksState.fetchBlocks()])
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
  const result = await generationState.generate()
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
          <ScheduledBlockList
            :blocks="blocksState.sortedBlocks.value"
            :loading="blocksState.loading.value"
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
  grid-template-columns: minmax(0, 1.35fr) minmax(340px, 0.65fr);
}

.main-column,
.side-column {
  display: grid;
  gap: 16px;
}

.empty-state {
  border: 1px dashed #c4b5fd;
}

@media (max-width: 1100px) {
  .scheduler-grid {
    grid-template-columns: 1fr;
  }
}
</style>
