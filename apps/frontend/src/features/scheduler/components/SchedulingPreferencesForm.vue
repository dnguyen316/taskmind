<script setup lang="ts">
import { computed, reactive, watch } from 'vue'
import type { SchedulingPreferences, UpdateSchedulingPreferencesPayload } from '../types'

const props = defineProps<{
  preferences: SchedulingPreferences | null
  loading?: boolean
  saving?: boolean
}>()

const emit = defineEmits<{
  submit: [payload: UpdateSchedulingPreferencesPayload]
}>()

const form = reactive({
  workdayStart: '09:00:00',
  workdayEnd: '17:00:00',
  blockGranularityMinutes: 30,
  maxDailyFocusMinutes: 360,
})

const updatedAtLabel = computed(() =>
  props.preferences ? new Date(props.preferences.updatedAt).toLocaleString() : 'Not loaded yet',
)

watch(
  () => props.preferences,
  (preferences) => {
    if (!preferences) return
    form.workdayStart = toTimeInputValue(preferences.workdayStart)
    form.workdayEnd = toTimeInputValue(preferences.workdayEnd)
    form.blockGranularityMinutes = preferences.blockGranularityMinutes
    form.maxDailyFocusMinutes = preferences.maxDailyFocusMinutes
  },
  { immediate: true },
)

function submitForm() {
  emit('submit', {
    version: props.preferences?.version ?? null,
    workdayStart: toApiTimeValue(form.workdayStart),
    workdayEnd: toApiTimeValue(form.workdayEnd),
    blockGranularityMinutes: form.blockGranularityMinutes,
    maxDailyFocusMinutes: form.maxDailyFocusMinutes,
  })
}

function toTimeInputValue(value: string) {
  return value.slice(0, 5)
}

function toApiTimeValue(value: string) {
  return value.length === 5 ? `${value}:00` : value
}
</script>

<template>
  <a-card class="tm-card-surface" title="Scheduling preferences" :loading="props.loading">
    <template #extra>
      <a-tag color="blue">Updated {{ updatedAtLabel }}</a-tag>
    </template>

    <a-form layout="vertical" class="preferences-form" @finish="submitForm">
      <div class="form-grid">
        <a-form-item label="Workday starts" required>
          <input v-model="form.workdayStart" class="time-input" type="time" />
        </a-form-item>
        <a-form-item label="Workday ends" required>
          <input v-model="form.workdayEnd" class="time-input" type="time" />
        </a-form-item>
        <a-form-item label="Block granularity" required>
          <a-input-number
            v-model:value="form.blockGranularityMinutes"
            :min="15"
            :max="240"
            :step="15"
            addon-after="minutes"
          />
        </a-form-item>
        <a-form-item label="Max daily focus" required>
          <a-input-number
            v-model:value="form.maxDailyFocusMinutes"
            :min="15"
            :max="1440"
            :step="15"
            addon-after="minutes"
          />
        </a-form-item>
      </div>

      <div class="form-footer">
        <p>
          Version {{ props.preferences?.version ?? '—' }} is sent with saves to protect against
          stale edits.
        </p>
        <a-button type="primary" html-type="submit" :loading="props.saving"
          >Save preferences</a-button
        >
      </div>
    </a-form>
  </a-card>
</template>

<style scoped>
.preferences-form {
  display: grid;
  gap: 10px;
}

.form-grid {
  display: grid;
  gap: 12px;
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.time-input {
  border: 1px solid #d9d9d9;
  border-radius: 8px;
  color: #1f2937;
  min-height: 32px;
  padding: 4px 11px;
  width: 100%;
}

.form-footer {
  align-items: center;
  display: flex;
  gap: 12px;
  justify-content: space-between;
}

.form-footer p {
  color: #64748b;
  margin: 0;
}

@media (max-width: 800px) {
  .form-grid {
    grid-template-columns: 1fr;
  }

  .form-footer {
    align-items: stretch;
    flex-direction: column;
  }
}
</style>
