<script setup lang="ts">
import { computed } from 'vue'
import { TASK_STATUS_SELECT_OPTIONS } from '../constants/taskPresentation'
import type { Project } from '../../projects/types'
import type { SavedTaskView, TaskFilters, TaskSortBy } from '../types'

const props = defineProps<{
  filters: TaskFilters
  projects: Project[]
  savedViews: SavedTaskView[]
}>()

const emit = defineEmits<{
  refresh: []
  applySavedView: [view: SavedTaskView]
  saveView: [name: string]
}>()

const statusOptions = computed(() => TASK_STATUS_SELECT_OPTIONS)
const projectOptions = computed(() =>
  props.projects.map((project) => ({ label: project.name, value: project.id })),
)
const sortOptions: { label: string; value: TaskSortBy }[] = [
  { label: 'Recently updated', value: 'updatedAt' },
  { label: 'Due date', value: 'dueAt' },
  { label: 'Priority', value: 'priority' },
  { label: 'Created', value: 'createdAt' },
]
const savedViewOptions = computed(() =>
  props.savedViews.map((view) => ({
    label: view.builtIn ? `★ ${view.name}` : view.name,
    value: view.id,
  })),
)
function handleSavedViewChange(viewId: string) {
  const view = props.savedViews.find((candidate) => candidate.id === viewId)
  if (view) emit('applySavedView', view)
}
function saveView() {
  const name = window.prompt('Saved view name')?.trim()
  if (name) emit('saveView', name)
}
</script>

<template>
  <a-row :gutter="12" class="filters-row">
    <a-col :xs="24" :md="6">
      <a-select
        :options="savedViewOptions"
        placeholder="Built-in and saved views"
        style="width: 100%"
        @change="handleSavedViewChange"
      />
    </a-col>
    <a-col :xs="24" :md="3">
      <a-button block @click="saveView">Save view</a-button>
    </a-col>
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
      <a-input
        v-model:value="props.filters.assigneeId"
        placeholder="Assignee ID"
        @change="emit('refresh')"
      />
    </a-col>
    <a-col :xs="12" :md="3">
      <a-input-number
        v-model:value="props.filters.priority"
        :min="1"
        :max="4"
        placeholder="Priority"
        style="width: 100%"
        @change="emit('refresh')"
      />
    </a-col>
    <a-col :xs="24" :md="8" class="quick-toggles">
      <a-checkbox v-model:checked="props.filters.dueToday" @change="emit('refresh')"
        >Today</a-checkbox
      >
      <a-checkbox v-model:checked="props.filters.blocked" @change="emit('refresh')"
        >Blocked</a-checkbox
      >
      <a-checkbox v-model:checked="props.filters.unassigned" @change="emit('refresh')"
        >Unassigned</a-checkbox
      >
      <a-checkbox v-model:checked="props.filters.noDueDate" @change="emit('refresh')"
        >No due date</a-checkbox
      >
      <a-checkbox v-model:checked="props.filters.stale" @change="emit('refresh')">Stale</a-checkbox>
      <a-checkbox v-model:checked="props.filters.archived" @change="emit('refresh')"
        >Archived</a-checkbox
      >
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
.quick-toggles {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  align-items: center;
}
.client-filter-note {
  color: #94a3b8;
  font-size: 11px;
  margin-top: 4px;
}
</style>
