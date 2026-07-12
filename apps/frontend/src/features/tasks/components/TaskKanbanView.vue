<script setup lang="ts">
import { computed } from 'vue'
import TaskStatusChip from './TaskStatusChip.vue'
import { TASK_STATUS_SELECT_OPTIONS } from '../constants/taskPresentation'
import { formatDateTime, isTaskOverdue } from '../utils/taskDates'
import { taskDetailRoute } from '../utils/taskRoutes'
import type { Task, TaskStatus } from '../types'

const props = defineProps<{
  tasks: Task[]
  pendingStatusTaskIds?: string[]
  loading?: boolean
  projectId?: string
}>()
const emit = defineEmits<{
  changeStatus: [taskId: string, status: TaskStatus, version: number | null]
}>()

const statuses: Array<{ value: TaskStatus; label: string; className: string }> = [
  { value: 'TODO', label: 'To do', className: 'todo' },
  { value: 'IN_PROGRESS', label: 'In progress', className: 'progress' },
  { value: 'DONE', label: 'Done', className: 'done' },
  { value: 'ARCHIVED', label: 'Archived', className: 'archived' },
]

const pendingStatusTaskIds = computed(() => new Set(props.pendingStatusTaskIds ?? []))
const tasksByStatus = computed(() =>
  statuses.map((status) => ({
    ...status,
    tasks: props.tasks.filter((task) => task.status === status.value),
  })),
)

function isStatusPending(taskId: string) {
  return pendingStatusTaskIds.value.has(taskId)
}

function handleStatusChange(task: Task, nextStatus: TaskStatus) {
  if (nextStatus !== task.status) {
    emit('changeStatus', task.id, nextStatus, task.version)
  }
}

function dueLabel(task: Task) {
  return task.dueAt ? formatDateTime(task.dueAt) : 'No due date'
}
</script>

<template>
  <a-spin :spinning="Boolean(loading)">
    <section class="tm-kanban" aria-label="Task kanban board">
      <article
        v-for="column in tasksByStatus"
        :key="column.value"
        class="tm-kanban-col"
        :class="column.className"
      >
        <header class="tm-kanban-col-head">
          <span class="tm-kanban-col-rail" />
          <span class="tm-kanban-col-title">{{ column.label }}</span>
          <span class="tm-kanban-col-count">{{ column.tasks.length }}</span>
        </header>
        <div class="tm-kanban-col-body">
          <router-link
            v-for="task in column.tasks"
            :key="task.id"
            :to="taskDetailRoute(task, projectId)"
            class="tm-kanban-card"
          >
            <div class="tm-kanban-card-top">
              <span class="tm-kanban-type">P{{ task.priority }}</span>
              <span class="tm-kanban-card-key">{{ task.taskKey || task.id.slice(0, 8) }}</span>
            </div>
            <div class="tm-kanban-card-title">{{ task.title }}</div>
            <div class="tm-kanban-card-meta">
              <TaskStatusChip :status="task.status" />
              <span class="tm-kanban-due" :class="{ overdue: isTaskOverdue(task) }">{{
                dueLabel(task)
              }}</span>
              <span class="tm-kanban-card-spacer" />
              <a-select
                :value="task.status"
                :options="TASK_STATUS_SELECT_OPTIONS"
                :disabled="isStatusPending(task.id)"
                :loading="isStatusPending(task.id)"
                size="small"
                aria-label="Task status"
                class="tm-kanban-status"
                @click.stop.prevent
                @change="(status: TaskStatus) => handleStatusChange(task, status)"
              />
            </div>
          </router-link>
          <a-empty
            v-if="column.tasks.length === 0"
            class="tm-kanban-empty"
            description="No tasks"
          />
        </div>
      </article>
    </section>
  </a-spin>
</template>

<style scoped>
.tm-kanban-status {
  width: 132px;
}
.overdue {
  color: #cf1322;
  font-weight: 600;
}
.tm-kanban-col.archived .tm-kanban-col-rail {
  background: #722ed1;
}
.tm-kanban-col.archived .tm-kanban-col-title {
  color: #722ed1;
}
</style>
