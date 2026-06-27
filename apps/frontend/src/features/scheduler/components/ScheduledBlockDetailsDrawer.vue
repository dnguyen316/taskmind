<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { CheckCircleOutlined } from '@ant-design/icons-vue'
import type { ScheduledBlock, UpdateScheduledBlockPayload } from '../types'
import {
  formatRange,
  fromDateTimeLocal,
  statusColor,
  statusLabel,
  toDateTimeLocal,
} from '../utils/scheduledBlockDisplay'

const props = withDefaults(
  defineProps<{
    block: ScheduledBlock | null
    open?: boolean
    saving?: boolean
    inline?: boolean
  }>(),
  { open: false, saving: false, inline: false },
)

const emit = defineEmits<{
  'update:open': [open: boolean]
  complete: [blockId: string]
  reschedule: [blockId: string, payload: UpdateScheduledBlockPayload]
}>()

const editValues = reactive({ startsAt: '', endsAt: '', rationale: '' })
const validationMessage = ref('')
const canEditBlock = computed(() => props.block?.status !== 'COMPLETED')
const drawerOpen = computed({
  get: () => props.open,
  set: (value: boolean) => emit('update:open', value),
})

watch(() => props.block, resetEditValues, { immediate: true })

function resetEditValues() {
  validationMessage.value = ''
  if (!props.block) return
  editValues.startsAt = toDateTimeLocal(props.block.startsAt)
  editValues.endsAt = toDateTimeLocal(props.block.endsAt)
  editValues.rationale = props.block.rationale ?? ''
}

function parseDateTimeLocal(value: string) {
  if (!value.trim()) return null
  const parsedDate = new Date(value)
  if (Number.isNaN(parsedDate.getTime())) return null
  return parsedDate
}

function submitReschedule() {
  validationMessage.value = ''
  if (!props.block || !canEditBlock.value) return

  const startsAt = parseDateTimeLocal(editValues.startsAt)
  const endsAt = parseDateTimeLocal(editValues.endsAt)

  if (!startsAt || !endsAt) {
    validationMessage.value = 'Enter valid start and end times.'
    return
  }

  if (endsAt.getTime() <= startsAt.getTime()) {
    validationMessage.value = 'End time must be after start time.'
    return
  }

  emit('reschedule', props.block.id, {
    version: props.block.version,
    startsAt: fromDateTimeLocal(editValues.startsAt),
    endsAt: fromDateTimeLocal(editValues.endsAt),
    rationale: editValues.rationale.trim() || null,
  })
}

function completeBlock() {
  if (props.block) emit('complete', props.block.id)
}
</script>

<template>
  <div v-if="inline && block" class="block-details">
    <slot name="header" />
    <div class="block-status-row">
      <a-tag :color="statusColor(block.status)">{{ statusLabel(block.status) }}</a-tag>
      <span>Block {{ block.id.slice(0, 8) }} · Version {{ block.version }}</span>
    </div>
    <p v-if="block.rationale" class="rationale">{{ block.rationale }}</p>

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

    <div class="edit-grid">
      <label>
        <span>Start</span>
        <input v-model="editValues.startsAt" type="datetime-local" :disabled="!canEditBlock" />
      </label>
      <label>
        <span>End</span>
        <input v-model="editValues.endsAt" type="datetime-local" :disabled="!canEditBlock" />
      </label>
      <label class="rationale-input">
        <span>Rationale</span>
        <input
          v-model="editValues.rationale"
          maxlength="500"
          placeholder="Why this block moved"
          :disabled="!canEditBlock"
        />
      </label>
    </div>

    <a-alert v-if="validationMessage" type="error" show-icon :message="validationMessage" />

    <div class="block-actions">
      <a-button v-if="canEditBlock" :loading="saving" @click="submitReschedule">Save time</a-button>
      <a-button
        v-if="block.status !== 'COMPLETED'"
        type="primary"
        :loading="saving"
        @click="completeBlock"
      >
        <template #icon><CheckCircleOutlined /></template>
        Complete
      </a-button>
    </div>
  </div>

  <a-drawer
    v-else
    v-model:open="drawerOpen"
    title="Scheduled block"
    width="420"
    class="event-drawer"
  >
    <div v-if="block" class="block-details">
      <a-tag :color="statusColor(block.status)">{{ statusLabel(block.status) }}</a-tag>
      <h3>Task {{ block.taskId.slice(0, 8) }}</h3>
      <p>{{ formatRange(block) }}</p>
      <p v-if="block.rationale" class="rationale">{{ block.rationale }}</p>
      <a-alert
        v-if="block.status === 'MISSED'"
        type="warning"
        show-icon
        message="Missed block"
        description="Edit this time or generate a schedule to get a reschedule proposal."
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

      <div class="edit-grid single-column">
        <label
          ><span>Start</span
          ><input v-model="editValues.startsAt" type="datetime-local" :disabled="!canEditBlock"
        /></label>
        <label
          ><span>End</span
          ><input v-model="editValues.endsAt" type="datetime-local" :disabled="!canEditBlock"
        /></label>
        <label
          ><span>Rationale</span
          ><input v-model="editValues.rationale" maxlength="500" :disabled="!canEditBlock"
        /></label>
      </div>

      <a-alert v-if="validationMessage" type="error" show-icon :message="validationMessage" />
    </div>

    <template #footer>
      <div v-if="block" class="block-actions">
        <a-button @click="drawerOpen = false">Close</a-button>
        <a-button v-if="canEditBlock" :loading="saving" @click="submitReschedule"
          >Save time</a-button
        >
        <a-button
          v-if="block.status !== 'COMPLETED'"
          type="primary"
          :loading="saving"
          @click="completeBlock"
        >
          <template #icon><CheckCircleOutlined /></template>
          Complete
        </a-button>
      </div>
    </template>
  </a-drawer>
</template>

<style scoped>
.block-details {
  display: grid;
  gap: 12px;
}
.block-status-row {
  align-items: center;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.block-status-row span,
.rationale {
  color: #64748b;
  font-size: 12px;
}
.rationale {
  font-size: 14px;
  margin: 0;
}
.edit-grid {
  display: grid;
  gap: 10px;
  grid-template-columns: repeat(2, minmax(0, 1fr));
}
.edit-grid.single-column {
  grid-template-columns: 1fr;
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
