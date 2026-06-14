<script setup lang="ts">
import { ref } from 'vue'
import { useCapture } from '../composables/useCapture'
import type { CapturedTaskDraft } from '../composables/types'

const text = ref('')
const draftStates = ref<Record<string, string>>({})
const rejectReason = ref('Not useful')
const { loading, accepting, rejecting, result, capture, acceptDraft, rejectDraft } = useCapture()

function draftKey(draft: CapturedTaskDraft, index: number) {
  return `${index}:${draft.title}`
}

async function accept(draft: CapturedTaskDraft, index: number) {
  const response = await acceptDraft({ draft })
  draftStates.value[draftKey(draft, index)] = `Accepted as task ${response.taskId}`
}

async function reject(draft: CapturedTaskDraft, index: number) {
  await rejectDraft({ draft, reason: rejectReason.value })
  draftStates.value[draftKey(draft, index)] = 'Rejected'
}
</script>

<template>
  <a-card title="AI capture">
    <a-textarea v-model:value="text" :rows="4" placeholder="Paste messy notes or action items" />
    <a-button type="primary" :loading="loading" :disabled="!text.trim()" @click="capture(text)"
      >Draft tasks</a-button
    >
    <a-list v-if="result" :data-source="result.drafts" bordered>
      <template #renderItem="{ item, index }">
        <a-list-item>
          <template #actions>
            <a-button size="small" type="primary" :loading="accepting" @click="accept(item, index)">
              Accept
            </a-button>
            <a-button size="small" danger :loading="rejecting" @click="reject(item, index)">
              Reject
            </a-button>
          </template>
          <a-list-item-meta>
            <template #title>{{ item.title }}</template>
            <template #description>
              {{ item.durationMinutes }} min · priority {{ item.priority }} · confidence
              {{ Math.round(item.confidence * 100) }}%
              <div v-if="draftStates[draftKey(item, index)]">
                {{ draftStates[draftKey(item, index)] }}
              </div>
            </template>
          </a-list-item-meta>
        </a-list-item>
      </template>
    </a-list>
  </a-card>
</template>
