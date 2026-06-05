<script setup lang="ts">
import { computed } from 'vue'
import { TASK_STATUS_OPTIONS } from '../constants/taskConstants'
import type { TaskFilters } from '../types'

const props = defineProps<{
  filters: TaskFilters
}>()

const emit = defineEmits<{
  refresh: []
}>()

const statusOptions = computed(() =>
  TASK_STATUS_OPTIONS.map((status) => ({ label: status.replace('_', ' '), value: status })),
)
</script>

<template>
  <a-row :gutter="12" class="filters-row">
    <a-col :xs="24" :md="8" :xl="7">
      <a-input-search placeholder="Search tasks by title or ID..." disabled />
    </a-col>
    <a-col :xs="12" :md="4">
      <a-select
        v-model:value="props.filters.status"
        :allow-clear="true"
        :options="statusOptions"
        placeholder="Status"
        style="width: 100%"
        @change="emit('refresh')"
      />
    </a-col>
    <a-col :xs="12" :md="4">
      <a-select placeholder="Priority" disabled style="width: 100%" />
    </a-col>
    <a-col :xs="12" :md="4">
      <a-select placeholder="Project" disabled style="width: 100%" />
    </a-col>
    <a-col :xs="12" :md="4">
      <a-select placeholder="Assignee" disabled style="width: 100%" />
    </a-col>
    <a-col :xs="24" :md="1" class="toggle-wrap">
      <a-switch
        v-model:checked="props.filters.overdueOnly"
        size="small"
        @change="emit('refresh')"
      />
    </a-col>
  </a-row>
</template>

<style scoped>
.filters-row {
  margin-bottom: 12px;
}
.toggle-wrap {
  display: flex;
  align-items: center;
  justify-content: center;
}
</style>
