<script setup lang="ts">
import { computed } from 'vue'
import { TASK_STATUS_OPTIONS } from '../constants/taskConstants'
import type { Project } from '../../projects/types'
import type { TaskFilters, TaskSortBy } from '../types'

const props = defineProps<{
  filters: TaskFilters
  projects: Project[]
}>()

const emit = defineEmits<{
  refresh: []
}>()

const statusOptions = computed(() =>
  TASK_STATUS_OPTIONS.map((status) => ({ label: status.replace('_', ' '), value: status })),
)
const projectOptions = computed(() =>
  props.projects.map((project) => ({ label: project.name, value: project.id })),
)
const sortOptions: { label: string; value: TaskSortBy }[] = [
  { label: 'Recently updated', value: 'updatedAt' },
  { label: 'Due date', value: 'dueAt' },
  { label: 'Priority', value: 'priority' },
]
</script>

<template>
  <a-row :gutter="12" class="filters-row">
    <a-col :xs="24" :md="8" :xl="6">
      <a-input-search
        v-model:value="props.filters.searchText"
        placeholder="Filter loaded tasks by title, description, or ID"
        allow-clear
      />
      <div class="client-filter-note">Client-side filter</div>
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
      <a-select
        v-model:value="props.filters.projectId"
        :allow-clear="true"
        :options="projectOptions"
        placeholder="Project"
        style="width: 100%"
      />
      <div class="client-filter-note">Client-side filter</div>
    </a-col>
    <a-col :xs="12" :md="4">
      <a-select
        v-model:value="props.filters.sortBy"
        :allow-clear="true"
        :options="sortOptions"
        placeholder="Backend order"
        style="width: 100%"
      />
      <div class="client-filter-note">Client-side sort</div>
    </a-col>
    <a-col :xs="12" :md="3">
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
.client-filter-note {
  color: #94a3b8;
  font-size: 11px;
  margin-top: 4px;
}
</style>
