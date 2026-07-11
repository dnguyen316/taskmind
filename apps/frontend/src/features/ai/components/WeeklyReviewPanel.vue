<script setup lang="ts">
import { useWeeklyReview } from '../composables/useWeeklyReview'

const { loading, result, generate } = useWeeklyReview()
</script>

<template>
  <a-card title="Weekly review">
    <a-button :loading="loading" @click="generate()">Generate</a-button>
    <a-alert
      v-if="result?.degraded"
      class="ai-fallback-warning"
      type="warning"
      show-icon
      message="Generated in degraded mode"
      description="Nova was unavailable or returned an unexpected response, so TaskMind used a local fallback review."
    />
    <p v-if="result">{{ result.summary }}</p>
  </a-card>
</template>
