<script setup>
import { TASK_STATUS_OPTIONS } from '../constants/taskConstants'

const props = defineProps({
  filters: {
    type: Object,
    required: true,
  },
})

const emit = defineEmits(['refresh'])
</script>

<template>
  <a-space wrap>
    <a-select
      v-model:value="props.filters.status"
      :allow-clear="true"
      placeholder="Filter status"
      style="min-width: 180px"
      @change="emit('refresh')"
    >
      <a-select-option v-for="status in TASK_STATUS_OPTIONS" :key="status" :value="status">
        {{ status }}
      </a-select-option>
    </a-select>

    <a-switch v-model:checked="props.filters.overdueOnly" @change="emit('refresh')" />
    <span>Overdue only</span>

    <a-button @click="emit('refresh')">Refresh</a-button>
  </a-space>
</template>
