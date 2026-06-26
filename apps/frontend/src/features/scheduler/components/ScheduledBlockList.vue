<script setup lang="ts">
import { computed } from 'vue'
import { ClockCircleOutlined } from '@ant-design/icons-vue'
import ScheduledBlockDetailsDrawer from './ScheduledBlockDetailsDrawer.vue'
import type { ScheduledBlock, UpdateScheduledBlockPayload } from '../types'
import {
  durationLabel,
  formatRange,
  statusColor,
  statusLabel,
} from '../utils/scheduledBlockDisplay'

const props = defineProps<{
  blocks: ScheduledBlock[]
  loading?: boolean
  savingBlockIds: Set<string>
}>()

const emit = defineEmits<{
  complete: [blockId: string]
  reschedule: [blockId: string, payload: UpdateScheduledBlockPayload]
}>()

const sortedBlocks = computed(() =>
  [...props.blocks].sort((a, b) => new Date(a.startsAt).getTime() - new Date(b.startsAt).getTime()),
)

function forwardReschedule(blockId: string, payload: UpdateScheduledBlockPayload) {
  emit('reschedule', blockId, payload)
}
</script>

<template>
  <a-card class="tm-card-surface" title="Scheduled blocks" :loading="props.loading">
    <template #extra>
      <a-tag :color="sortedBlocks.length ? 'blue' : 'default'"
        >{{ sortedBlocks.length }} blocks</a-tag
      >
    </template>

    <a-empty
      v-if="!sortedBlocks.length"
      description="No scheduled blocks yet. Generate a schedule to create calendar blocks from your current tasks."
    />

    <div v-else class="block-list">
      <article
        v-for="block in sortedBlocks"
        :key="block.id"
        class="block-card"
        :class="`status-${block.status.toLowerCase()}`"
      >
        <div class="block-summary">
          <div class="block-icon"><ClockCircleOutlined /></div>
          <div>
            <div class="block-title-row">
              <strong>Task {{ block.taskId.slice(0, 8) }}</strong>
              <a-tag :color="statusColor(block.status)">{{ statusLabel(block.status) }}</a-tag>
            </div>
            <p>{{ formatRange(block) }} · {{ durationLabel(block) }}</p>
          </div>
        </div>

        <ScheduledBlockDetailsDrawer
          inline
          :block="block"
          :saving="props.savingBlockIds.has(block.id)"
          @complete="emit('complete', $event)"
          @reschedule="forwardReschedule"
        />
      </article>
    </div>
  </a-card>
</template>

<style scoped>
.block-list {
  display: grid;
  gap: 14px;
}

.block-card {
  border: 1px solid #e2e8f0;
  border-radius: 16px;
  display: grid;
  gap: 12px;
  padding: 16px;
}

.block-card.status-missed {
  border-color: #f59e0b;
}

.block-card.status-completed {
  border-color: #86efac;
}

.block-summary {
  align-items: flex-start;
  display: flex;
  gap: 12px;
}

.block-icon {
  background: #eef2ff;
  border-radius: 12px;
  color: #4f46e5;
  display: grid;
  flex: 0 0 auto;
  height: 40px;
  place-items: center;
  width: 40px;
}

.block-title-row {
  align-items: center;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.block-summary p {
  color: #475569;
  margin: 6px 0;
}
</style>
