<script setup lang="ts">
import { ref } from 'vue'
import { useGoalBreakdown } from '../composables/useGoalBreakdown'

const goalId = ref('')
const { loading, result, breakdown } = useGoalBreakdown()
</script>

<template>
  <a-card title="Goal breakdown">
    <a-input v-model:value="goalId" placeholder="Goal UUID" />
    <a-button :loading="loading" :disabled="!goalId" @click="breakdown(goalId, {})"
      >Break down</a-button
    >
    <ul v-if="result">
      <li v-for="task in result.tasks" :key="task.title">
        {{ task.title }} — {{ task.rationale }}
      </li>
    </ul>
  </a-card>
</template>
