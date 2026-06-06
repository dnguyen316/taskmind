import { ref } from 'vue'
import { generateSchedule } from '../api/schedulerApi'
import type { GenerateSchedulePayload, GenerateScheduleResponse } from '../types'

export function useScheduleGeneration() {
  const generating = ref(false)
  const errorMessage = ref('')
  const generationResult = ref<GenerateScheduleResponse | null>(null)

  async function generate(payload?: GenerateSchedulePayload) {
    generating.value = true
    errorMessage.value = ''

    try {
      generationResult.value = await generateSchedule(payload)
      return generationResult.value
    } catch (error: unknown) {
      errorMessage.value = error instanceof Error ? error.message : 'Failed to generate schedule.'
      throw error
    } finally {
      generating.value = false
    }
  }

  return {
    generating,
    errorMessage,
    generationResult,
    generate,
  }
}
