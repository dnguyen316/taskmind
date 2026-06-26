import { computed, ref } from 'vue'
import {
  completeScheduledBlock,
  listScheduledBlocks,
  updateScheduledBlock,
} from '../api/schedulerApi'
import type { GenerateSchedulePayload, ScheduledBlock, UpdateScheduledBlockPayload } from '../types'

export function useScheduledBlocks() {
  const blocks = ref<ScheduledBlock[]>([])
  const loading = ref(false)
  const savingBlockIds = ref<Set<string>>(new Set())
  const errorMessage = ref('')
  let fetchRequestId = 0

  const sortedBlocks = computed(() => [...blocks.value].sort(compareBlocks))
  const missedBlocks = computed(() =>
    sortedBlocks.value.filter((block) => block.status === 'MISSED'),
  )
  const hasBlocks = computed(() => blocks.value.length > 0)

  async function fetchBlocks(params?: GenerateSchedulePayload) {
    const currentRequestId = ++fetchRequestId
    loading.value = true
    errorMessage.value = ''

    try {
      const fetchedBlocks = await listScheduledBlocks(params)

      if (currentRequestId === fetchRequestId) {
        blocks.value = fetchedBlocks
      }

      return fetchedBlocks
    } catch (error: unknown) {
      if (currentRequestId === fetchRequestId) {
        errorMessage.value =
          error instanceof Error ? error.message : 'Failed to load scheduled blocks.'
      }

      throw error
    } finally {
      if (currentRequestId === fetchRequestId) {
        loading.value = false
      }
    }
  }

  async function saveBlock(blockId: string, payload: UpdateScheduledBlockPayload) {
    markSaving(blockId, true)
    errorMessage.value = ''

    try {
      const updatedBlock = await updateScheduledBlock(blockId, payload)
      upsertBlock(updatedBlock)
      return updatedBlock
    } catch (error: unknown) {
      errorMessage.value =
        error instanceof Error ? error.message : 'Failed to update scheduled block.'
      throw error
    } finally {
      markSaving(blockId, false)
    }
  }

  async function completeBlock(blockId: string) {
    markSaving(blockId, true)
    errorMessage.value = ''

    try {
      const updatedBlock = await completeScheduledBlock(blockId)
      upsertBlock(updatedBlock)
      return updatedBlock
    } catch (error: unknown) {
      errorMessage.value =
        error instanceof Error ? error.message : 'Failed to complete scheduled block.'
      throw error
    } finally {
      markSaving(blockId, false)
    }
  }

  function mergeBlocks(nextBlocks: ScheduledBlock[]) {
    nextBlocks.forEach(upsertBlock)
  }

  function upsertBlock(nextBlock: ScheduledBlock) {
    const index = blocks.value.findIndex((block) => block.id === nextBlock.id)

    if (index >= 0) {
      blocks.value[index] = nextBlock
      return
    }

    blocks.value = [...blocks.value, nextBlock]
  }

  function markSaving(blockId: string, saving: boolean) {
    const nextSavingIds = new Set(savingBlockIds.value)

    if (saving) {
      nextSavingIds.add(blockId)
    } else {
      nextSavingIds.delete(blockId)
    }

    savingBlockIds.value = nextSavingIds
  }

  return {
    blocks,
    sortedBlocks,
    missedBlocks,
    loading,
    savingBlockIds,
    errorMessage,
    hasBlocks,
    fetchBlocks,
    saveBlock,
    completeBlock,
    mergeBlocks,
  }
}

function compareBlocks(blockA: ScheduledBlock, blockB: ScheduledBlock) {
  return new Date(blockA.startsAt).getTime() - new Date(blockB.startsAt).getTime()
}
