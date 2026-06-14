<script setup lang="ts">
import { ref } from 'vue'
import { useWeeklyReview } from '../composables/useWeeklyReview'

const userId = ref('')
const { loading, result, generate } = useWeeklyReview()
</script>

<template>
  <a-card title="Weekly review">
    <a-input v-model:value="userId" placeholder="User UUID" />
    <a-button :loading="loading" :disabled="!userId" @click="generate(userId)">Generate</a-button>
    <p v-if="result">{{ result.summary }}</p>
  </a-card>
</template>
