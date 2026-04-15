<script setup>
import { TASK_STATUS_OPTIONS } from '../constants/taskConstants'

defineProps({
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
      v-model:value="filters.status"
      style="width: 180px"
      :options="[{ value: '', label: 'All statuses' }, ...TASK_STATUS_OPTIONS.map((value) => ({ value, label: value }))]"
      @change="emit('refresh')"
    />

    <a-switch
      v-model:checked="filters.overdueOnly"
      checked-children="Overdue only"
      un-checked-children="All due states"
      @change="emit('refresh')"
    />

    <a-button @click="emit('refresh')">Refresh</a-button>
  </a-space>
</template>
