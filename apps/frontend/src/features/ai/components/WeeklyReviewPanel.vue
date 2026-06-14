<script setup lang="ts">
import { useWeeklyReview } from '../composables/useWeeklyReview'

const { loading, result, generate } = useWeeklyReview()
</script>

<template>
  <a-card title="Weekly review">
    <a-button :loading="loading" @click="generate()">Generate</a-button>
    <p v-if="result">{{ result.summary }}</p>
  </a-card>
</template>
