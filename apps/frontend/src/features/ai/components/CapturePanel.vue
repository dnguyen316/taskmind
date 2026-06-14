<script setup lang="ts">
import { ref } from 'vue'
import { useCapture } from '../composables/useCapture'

const text = ref('')
const { loading, result, capture } = useCapture()
</script>

<template>
  <a-card title="AI capture">
    <a-textarea v-model:value="text" :rows="4" placeholder="Paste messy notes or action items" />
    <a-button type="primary" :loading="loading" :disabled="!text.trim()" @click="capture(text)"
      >Draft tasks</a-button
    >
    <a-list v-if="result" :data-source="result.drafts" bordered>
      <template #renderItem="{ item }"
        ><a-list-item>{{ item.title }} · {{ item.durationMinutes }} min</a-list-item></template
      >
    </a-list>
  </a-card>
</template>
