<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useCurrentUserId } from '../../../composables/useCurrentUserId'
import { listProjects } from '../../projects/api/projectsApi'
import type { Project } from '../../projects/types'
import { listTasks } from '../../tasks/api/tasksApi'
import type { Task } from '../../tasks/types'
import { useGoalBreakdown } from '../composables/useGoalBreakdown'

const goalId = ref('')
const tasks = ref<Task[]>([])
const projects = ref<Project[]>([])
const loadingGoals = ref(false)
const selectorError = ref('')
const permissionDenied = ref(false)
const { requireCurrentUserId } = useCurrentUserId()
const { loading, result, breakdown } = useGoalBreakdown()

const projectNames = computed(
  () => new Map(projects.value.map((project) => [project.id, project.name])),
)
const goalOptions = computed(() =>
  tasks.value.map((task) => ({
    value: task.id,
    label: `${task.title} · ${projectNames.value.get(task.projectId) ?? 'No project'} · ${task.status}`,
    task,
  })),
)

onMounted(() => {
  void loadGoalOptions()
})

async function loadGoalOptions() {
  loadingGoals.value = true
  selectorError.value = ''
  permissionDenied.value = false

  try {
    const userId = requireCurrentUserId()
    const [projectList, taskList] = await Promise.all([
      listProjects(),
      listTasks({ userId, filters: { overdueOnly: false, searchText: '' }, size: 100 }),
    ])
    projects.value = projectList
    tasks.value = taskList.filter((task) => task.status !== 'ARCHIVED' && !task.deletedAt)
  } catch (error: unknown) {
    const message = error instanceof Error ? error.message : 'Failed to load available goals.'
    permissionDenied.value = /403|forbidden|permission|not authorized/i.test(message)
    selectorError.value = permissionDenied.value
      ? 'You do not have permission to browse goals.'
      : message
  } finally {
    loadingGoals.value = false
  }
}
</script>

<template>
  <a-card title="Goal breakdown">
    <a-space direction="vertical" style="width: 100%" size="middle">
      <a-alert v-if="permissionDenied" type="warning" show-icon :message="selectorError" />
      <a-alert v-else-if="selectorError" type="error" show-icon :message="selectorError" />
      <a-select
        v-model:value="goalId"
        show-search
        allow-clear
        option-filter-prop="label"
        placeholder="Search for a task or goal"
        :loading="loadingGoals"
        :disabled="loadingGoals || permissionDenied"
        :options="goalOptions"
      >
        <template #notFoundContent>
          <a-spin v-if="loadingGoals" size="small" />
          <span v-else-if="permissionDenied">Permission denied.</span>
          <span v-else>No available goals found.</span>
        </template>
      </a-select>
      <a-button :loading="loading" :disabled="!goalId" @click="breakdown(goalId, {})">
        Break down
      </a-button>
    </a-space>
    <ul v-if="result">
      <li v-for="task in result.tasks" :key="task.title">
        {{ task.title }} — {{ task.rationale }}
      </li>
    </ul>
  </a-card>
</template>
