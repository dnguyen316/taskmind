import { computed, reactive, ref } from 'vue'
import { getSchedulingPreferences, updateSchedulingPreferences } from '../api/schedulerApi'
import type { SchedulingPreferences, UpdateSchedulingPreferencesPayload } from '../types'

export function useSchedulingPreferences() {
  const preferences = ref<SchedulingPreferences | null>(null)
  const loading = ref(false)
  const saving = ref(false)
  const errorMessage = ref('')
  const successMessage = ref('')

  const form = reactive<UpdateSchedulingPreferencesPayload>({
    version: null,
    workdayStart: '09:00:00',
    workdayEnd: '17:00:00',
    blockGranularityMinutes: 30,
    maxDailyFocusMinutes: 360,
  })

  const hasPreferences = computed(() => Boolean(preferences.value))

  async function fetchPreferences() {
    loading.value = true
    errorMessage.value = ''

    try {
      preferences.value = await getSchedulingPreferences()
      applyPreferencesToForm(preferences.value)
      return preferences.value
    } catch (error: unknown) {
      errorMessage.value =
        error instanceof Error ? error.message : 'Failed to load scheduling preferences.'
      throw error
    } finally {
      loading.value = false
    }
  }

  async function savePreferences(payload: UpdateSchedulingPreferencesPayload = { ...form }) {
    saving.value = true
    errorMessage.value = ''
    successMessage.value = ''

    try {
      preferences.value = await updateSchedulingPreferences(payload)
      applyPreferencesToForm(preferences.value)
      successMessage.value = 'Scheduling preferences saved.'
      return preferences.value
    } catch (error: unknown) {
      errorMessage.value =
        error instanceof Error ? error.message : 'Failed to save scheduling preferences.'
      throw error
    } finally {
      saving.value = false
    }
  }

  function applyPreferencesToForm(nextPreferences: SchedulingPreferences) {
    form.version = nextPreferences.version
    form.workdayStart = nextPreferences.workdayStart
    form.workdayEnd = nextPreferences.workdayEnd
    form.blockGranularityMinutes = nextPreferences.blockGranularityMinutes
    form.maxDailyFocusMinutes = nextPreferences.maxDailyFocusMinutes
  }

  return {
    preferences,
    form,
    loading,
    saving,
    errorMessage,
    successMessage,
    hasPreferences,
    fetchPreferences,
    savePreferences,
  }
}
