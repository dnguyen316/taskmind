<script setup lang="ts">
import { computed, reactive } from 'vue'
import { CheckCircleOutlined, ClockCircleOutlined } from '@ant-design/icons-vue'
import type { ScheduledBlock, UpdateScheduledBlockPayload } from '../types'

const props = defineProps<{
  blocks: ScheduledBlock[]
  loading?: boolean
  savingBlockIds: Set<string>
}>()

const emit = defineEmits<{
  complete: [blockId: string]
  reschedule: [blockId: string, payload: UpdateScheduledBlockPayload]
}>()

const editValues = reactive<
  Record<string, { startsAt: string; endsAt: string; rationale: string }>
>({})

const sortedBlocks = computed(() =>
  [...props.blocks].sort((a, b) => new Date(a.startsAt).getTime() - new Date(b.startsAt).getTime()),
)

function ensureEditValues(block: ScheduledBlock) {
  if (!editValues[block.id]) {
    editValues[block.id] = {
      startsAt: toDateTimeLocal(block.startsAt),
      endsAt: toDateTimeLocal(block.endsAt),
      rationale: block.rationale ?? '',
    }
  }

  return editValues[block.id]
}

function statusColor(status: ScheduledBlock['status']) {
  if (status === 'COMPLETED') return 'green'
  if (status === 'MISSED') return 'red'
  if (status === 'CANCELLED') return 'default'
  return 'blue'
}

function statusLabel(status: ScheduledBlock['status']) {
  return status.replace('_', ' ')
}

function formatRange(block: ScheduledBlock) {
  return `${new Date(block.startsAt).toLocaleString()} → ${new Date(block.endsAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}`
}

function durationLabel(block: ScheduledBlock) {
  const durationMinutes = Math.round(
    (new Date(block.endsAt).getTime() - new Date(block.startsAt).getTime()) / 60_000,
  )
  return `${Math.max(durationMinutes, 0)} min`
}

function submitReschedule(block: ScheduledBlock) {
  const values = ensureEditValues(block)
  emit('reschedule', block.id, {
    version: block.version,
    startsAt: fromDateTimeLocal(values.startsAt),
    endsAt: fromDateTimeLocal(values.endsAt),
    rationale: values.rationale.trim() || null,
  })
}

function toDateTimeLocal(value: string) {
  const date = new Date(value)
  const timezoneOffsetMs = date.getTimezoneOffset() * 60_000
  return new Date(date.getTime() - timezoneOffsetMs).toISOString().slice(0, 16)
}

function fromDateTimeLocal(value: string) {
  return new Date(value).toISOString()
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
            <span>Block {{ block.id.slice(0, 8) }} · Version {{ block.version }}</span>
          </div>
        </div>

        <a-alert
          v-if="block.status === 'MISSED'"
          type="warning"
          show-icon
          message="Missed block"
          description="This block ended before completion. Generate a schedule to request a reschedule proposal or edit the time below."
        />
        <a-alert
          v-else-if="block.status === 'COMPLETED'"
          type="success"
          show-icon
          message="Completed"
          :description="
            block.completedAt
              ? `Completed ${new Date(block.completedAt).toLocaleString()}`
              : 'Completed block.'
          "
        />

        <p v-if="block.rationale" class="rationale">{{ block.rationale }}</p>

        <div class="edit-grid">
          <label>
            <span>Start</span>
            <input v-model="ensureEditValues(block).startsAt" type="datetime-local" />
          </label>
          <label>
            <span>End</span>
            <input v-model="ensureEditValues(block).endsAt" type="datetime-local" />
          </label>
          <label class="rationale-input">
            <span>Rationale</span>
            <input
              v-model="ensureEditValues(block).rationale"
              maxlength="500"
              placeholder="Why this block moved"
            />
          </label>
        </div>

        <div class="block-actions">
          <a-button :loading="props.savingBlockIds.has(block.id)" @click="submitReschedule(block)"
            >Save time</a-button
          >
          <a-button
            v-if="block.status !== 'COMPLETED'"
            type="primary"
            :loading="props.savingBlockIds.has(block.id)"
            @click="emit('complete', block.id)"
          >
            <template #icon><CheckCircleOutlined /></template>
            Complete
          </a-button>
        </div>
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

.block-summary p,
.rationale {
  color: #475569;
  margin: 6px 0;
}

.block-summary span {
  color: #64748b;
  font-size: 12px;
}

.edit-grid {
  display: grid;
  gap: 10px;
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.edit-grid label {
  display: grid;
  gap: 6px;
}

.edit-grid span {
  color: #475569;
  font-size: 12px;
  font-weight: 600;
}

.edit-grid input {
  border: 1px solid #d9d9d9;
  border-radius: 8px;
  min-height: 32px;
  padding: 4px 11px;
}

.rationale-input {
  grid-column: 1 / -1;
}

.block-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  justify-content: flex-end;
}

@media (max-width: 800px) {
  .edit-grid {
    grid-template-columns: 1fr;
  }

  .block-actions {
    justify-content: flex-start;
  }
}
</style>
