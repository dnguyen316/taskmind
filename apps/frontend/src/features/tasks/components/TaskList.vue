<script setup lang="ts">
import { computed } from 'vue'
import TaskStatusChip from './TaskStatusChip.vue'
import { TASK_STATUS_SELECT_OPTIONS } from '../constants/taskPresentation'
import { formatDateTime, isTaskOverdue, toTimestamp } from '../utils/taskDates'
import { taskDetailRoute as buildTaskDetailRoute } from '../utils/taskRoutes'
import type { Task, TaskStatus } from '../types'

const props = defineProps<{
  tasks: Task[]
  pendingStatusTaskIds?: string[]
  loading?: boolean
  errorMessage?: string
  projectId?: string
}>()
const emit = defineEmits<{ changeStatus: [taskId: string, status: TaskStatus] }>()

interface TaskTableColumn {
  title: string
  dataIndex: keyof Task | 'key'
  key: keyof Task | 'key'
  width?: number
}

interface TaskTableRecord extends Task {
  key: string
}

const columns: TaskTableColumn[] = [
  { title: 'ID', dataIndex: 'id', key: 'id', width: 120 },
  { title: 'TASK', dataIndex: 'title', key: 'title' },
  { title: 'STATUS', dataIndex: 'status', key: 'status', width: 180 },
  { title: 'PRIORITY', dataIndex: 'priority', key: 'priority', width: 110 },
  { title: 'DUE', dataIndex: 'dueAt', key: 'dueAt', width: 160 },
  { title: 'EFFORT', dataIndex: 'durationMinutes', key: 'durationMinutes', width: 90 },
]

const dataSource = computed<TaskTableRecord[]>(() =>
  props.tasks.map((task) => ({ ...task, key: task.id })),
)
const pendingStatusTaskIds = computed(() => new Set(props.pendingStatusTaskIds ?? []))

function isStatusPending(taskId: string) {
  return pendingStatusTaskIds.value.has(taskId)
}

function taskDetailRoute(task: TaskTableRecord) {
  return buildTaskDetailRoute(task, props.projectId)
}

function handleStatusChange(task: TaskTableRecord, nextStatus: TaskStatus) {
  if (nextStatus !== task.status) {
    emit('changeStatus', task.id, nextStatus)
  }
}

function dueLabel(task: Task) {
  if (!task.dueAt) return '—'
  const due = toTimestamp(task.dueAt)
  const now = Date.now()
  const dayMs = 1000 * 60 * 60 * 24
  const diffDays = Math.round((due - now) / dayMs)
  if (Math.abs(diffDays) <= 1)
    return diffDays < 0 ? 'Yesterday' : diffDays === 0 ? 'Today' : 'Tomorrow'
  return formatDateTime(task.dueAt)
}
</script>

<template>
  <a-table
    :columns="columns"
    :data-source="dataSource"
    :pagination="{ pageSize: 12 }"
    :loading="props.loading"
    size="middle"
  >
    <template #emptyText>
      <a-empty
        :description="
          props.errorMessage
            ? 'Failed to load tasks. Resolve the error above, then refresh.'
            : 'No tasks found for the current filters.'
        "
      />
    </template>
    <template #bodyCell="{ column, record }: { column: TaskTableColumn; record: TaskTableRecord }">
      <template v-if="column.key === 'id'">{{ record.taskKey || record.id.slice(0, 8) }}</template>
      <template v-else-if="column.key === 'title'">
        <router-link :to="taskDetailRoute(record)" class="task-link">{{
          record.title
        }}</router-link>
        <div class="desc">{{ record.description || 'No description' }}</div>
      </template>
      <template v-else-if="column.key === 'status'">
        <a-select
          :value="record.status"
          :options="TASK_STATUS_SELECT_OPTIONS"
          :disabled="isStatusPending(record.id)"
          :loading="isStatusPending(record.id)"
          aria-label="Task status"
          class="status-select"
          @change="(status: TaskStatus) => handleStatusChange(record, status)"
        >
          <template #option="{ value }">
            <TaskStatusChip :status="value as TaskStatus" />
          </template>
        </a-select>
        <TaskStatusChip :status="record.status" class="status-current-chip" />
      </template>
      <template v-else-if="column.key === 'priority'">P{{ record.priority }}</template>
      <template v-else-if="column.key === 'dueAt'">
        <span :class="{ overdue: isTaskOverdue(record) }">{{ dueLabel(record) }}</span>
      </template>
      <template v-else-if="column.key === 'durationMinutes'">{{
        Math.max(1, Math.round((record.durationMinutes || 30) / 30))
      }}</template>
    </template>
  </a-table>
</template>

<style scoped>
.task-link {
  font-weight: 600;
}
.desc {
  color: #64748b;
  font-size: 12px;
  margin-top: 4px;
}
.overdue {
  color: #cf1322;
  font-weight: 600;
}
.status-select {
  width: 140px;
}
</style>
