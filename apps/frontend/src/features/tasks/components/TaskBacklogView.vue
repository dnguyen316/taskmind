<script setup lang="ts">
import { computed } from 'vue'
import TaskStatusChip from './TaskStatusChip.vue'
import { TASK_STATUS_SELECT_OPTIONS } from '../constants/taskPresentation'
import { formatDateTime, isTaskOverdue, toTimestamp } from '../utils/taskDates'
import { taskDetailRoute } from '../utils/taskRoutes'
import type { Task, TaskStatus } from '../types'

const props = defineProps<{
  tasks: Task[]
  pendingStatusTaskIds?: string[]
  loading?: boolean
  projectId?: string
}>()
const emit = defineEmits<{ changeStatus: [taskId: string, status: TaskStatus] }>()

const pendingStatusTaskIds = computed(() => new Set(props.pendingStatusTaskIds ?? []))
const prioritizedTasks = computed(() =>
  [...props.tasks].sort((left, right) => {
    const priorityDiff = left.priority - right.priority
    if (priorityDiff !== 0) return priorityDiff
    return dueSortValue(left) - dueSortValue(right)
  }),
)

function dueSortValue(task: Task) {
  return task.dueAt ? toTimestamp(task.dueAt) : Number.MAX_SAFE_INTEGER
}

function isStatusPending(taskId: string) {
  return pendingStatusTaskIds.value.has(taskId)
}

function handleStatusChange(task: Task, nextStatus: TaskStatus) {
  if (nextStatus !== task.status) {
    emit('changeStatus', task.id, nextStatus)
  }
}

function effortLabel(task: Task) {
  return `${Math.max(1, Math.round((task.durationMinutes || 30) / 30))}u`
}
</script>

<template>
  <a-spin :spinning="Boolean(loading)">
    <section class="tm-backlog" aria-label="Prioritized task backlog">
      <article class="tm-backlog-section">
        <header class="tm-backlog-head">
          <div class="tm-backlog-head-left">
            <span class="tm-backlog-head-icon">☰</span>
            <div>
              <h2 class="tm-backlog-title">Prioritized backlog</h2>
              <p class="tm-backlog-subtitle">Sorted by priority, then due date.</p>
            </div>
          </div>
          <span class="tm-backlog-count">{{ prioritizedTasks.length }}</span>
        </header>
        <a-empty
          v-if="prioritizedTasks.length === 0"
          class="tm-backlog-empty"
          description="No tasks found for the current filters."
        />
        <div v-else class="tm-backlog-list">
          <router-link
            v-for="(task, index) in prioritizedTasks"
            :key="task.id"
            :to="taskDetailRoute(task, projectId)"
            class="tm-backlog-item"
          >
            <span class="tm-backlog-rank">{{ index + 1 }}</span>
            <span class="tm-type-badge">P{{ task.priority }}</span>
            <span class="tm-backlog-main">
              <span class="tm-backlog-item-title">{{ task.title }}</span>
              <span class="tm-backlog-item-meta">
                <TaskStatusChip :status="task.status" />
                <span class="tm-backlog-key">{{ task.taskKey || task.id.slice(0, 8) }}</span>
                <span class="tm-meta-sep" />
                <span class="tm-backlog-due" :class="{ overdue: isTaskOverdue(task) }">
                  {{ task.dueAt ? formatDateTime(task.dueAt) : 'No due date' }}
                </span>
              </span>
            </span>
            <span class="tm-backlog-right">
              <span class="tm-backlog-effort">{{ effortLabel(task) }}</span>
              <a-select
                :value="task.status"
                :options="TASK_STATUS_SELECT_OPTIONS"
                :disabled="isStatusPending(task.id)"
                :loading="isStatusPending(task.id)"
                size="small"
                aria-label="Task status"
                class="tm-backlog-status"
                @click.stop.prevent
                @change="(status: TaskStatus) => handleStatusChange(task, status)"
              />
            </span>
          </router-link>
        </div>
      </article>
    </section>
  </a-spin>
</template>

<style scoped>
.tm-backlog-status {
  width: 132px;
}
.overdue {
  color: #cf1322;
  font-weight: 600;
}
</style>
