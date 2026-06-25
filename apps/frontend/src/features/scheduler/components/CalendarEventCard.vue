<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { CheckCircleOutlined } from '@ant-design/icons-vue'
import type { ScheduledBlock, UpdateScheduledBlockPayload } from '../types'

const props = defineProps<{ block: ScheduledBlock; compact?: boolean; saving?: boolean }>()
const emit = defineEmits<{
  complete: [blockId: string]
  reschedule: [blockId: string, payload: UpdateScheduledBlockPayload]
}>()

const drawerOpen = ref(false)
const editValues = reactive({ startsAt: '', endsAt: '', rationale: '' })

const statusColor = computed(() => {
  if (props.block.status === 'COMPLETED') return 'green'
  if (props.block.status === 'MISSED') return 'orange'
  if (props.block.status === 'CANCELLED') return 'default'
  return 'blue'
})
const cardClass = computed(() => `status-${props.block.status.toLowerCase()}`)
const timeLabel = computed(
  () =>
    `${new Date(props.block.startsAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })} – ${new Date(props.block.endsAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}`,
)
const durationLabel = computed(() => {
  const minutes = Math.max(
    0,
    Math.round(
      (new Date(props.block.endsAt).getTime() - new Date(props.block.startsAt).getTime()) / 60_000,
    ),
  )
  return `${minutes} min`
})

watch(() => props.block, resetEditValues, { immediate: true })

function resetEditValues() {
  editValues.startsAt = toDateTimeLocal(props.block.startsAt)
  editValues.endsAt = toDateTimeLocal(props.block.endsAt)
  editValues.rationale = props.block.rationale ?? ''
}

function submitReschedule() {
  emit('reschedule', props.block.id, {
    version: props.block.version,
    startsAt: fromDateTimeLocal(editValues.startsAt),
    endsAt: fromDateTimeLocal(editValues.endsAt),
    rationale: editValues.rationale.trim() || null,
  })
}

function toDateTimeLocal(value: string) {
  const date = new Date(value)
  return new Date(date.getTime() - date.getTimezoneOffset() * 60_000).toISOString().slice(0, 16)
}

function fromDateTimeLocal(value: string) {
  return new Date(value).toISOString()
}
</script>

<template>
  <button class="calendar-event-card" :class="[cardClass, { compact }]" @click="drawerOpen = true">
    <span class="event-time">{{ timeLabel }}</span>
    <strong>Task {{ block.taskId.slice(0, 8) }}</strong>
    <span v-if="!compact" class="event-meta">{{ block.status }} · {{ durationLabel }}</span>
  </button>

  <a-drawer v-model:open="drawerOpen" title="Scheduled block" width="420" class="event-drawer">
    <div class="event-details">
      <a-tag :color="statusColor">{{ block.status }}</a-tag>
      <h3>Task {{ block.taskId.slice(0, 8) }}</h3>
      <p>{{ new Date(block.startsAt).toLocaleString() }} → {{ timeLabel.split('–')[1].trim() }}</p>
      <p v-if="block.rationale" class="muted">{{ block.rationale }}</p>
      <a-alert
        v-if="block.status === 'MISSED'"
        type="warning"
        show-icon
        message="Missed block"
        description="Edit this time or generate a schedule to get a reschedule proposal."
      />
      <a-alert
        v-if="block.status === 'COMPLETED'"
        type="success"
        show-icon
        message="Completed"
        :description="
          block.completedAt ? new Date(block.completedAt).toLocaleString() : 'Completed block.'
        "
      />

      <label><span>Start</span><input v-model="editValues.startsAt" type="datetime-local" /></label>
      <label><span>End</span><input v-model="editValues.endsAt" type="datetime-local" /></label>
      <label><span>Rationale</span><input v-model="editValues.rationale" maxlength="500" /></label>
    </div>

    <template #footer>
      <div class="drawer-actions">
        <a-button @click="drawerOpen = false">Close</a-button>
        <a-button :loading="saving" @click="submitReschedule">Save time</a-button>
        <a-button
          v-if="block.status !== 'COMPLETED'"
          type="primary"
          :loading="saving"
          @click="emit('complete', block.id)"
        >
          <template #icon><CheckCircleOutlined /></template>
          Complete
        </a-button>
      </div>
    </template>
  </a-drawer>
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
.event-time,
.event-meta,
.muted {
  color: inherit;
  font-size: 12px;
  opacity: 0.78;
}
.event-details {
  display: grid;
  gap: 12px;
}
.event-details label {
  display: grid;
  gap: 6px;
}
.event-details span {
  font-size: 12px;
  font-weight: 600;
}
.event-details input {
  border: 1px solid #d9d9d9;
  border-radius: 8px;
  min-height: 36px;
  padding: 4px 11px;
}
.drawer-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  justify-content: flex-end;
}
</style>
