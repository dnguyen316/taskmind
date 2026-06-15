<script setup lang="ts">
import { computed } from 'vue'
import { formatDateTime, isTaskOverdue, toTimestamp } from '../utils/taskDates'
import type { Task, TaskStatus } from '../types'

const props = defineProps<{ tasks: Task[]; pendingStatusTaskIds?: string[] }>()
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

const statusOptions: { label: string; value: TaskStatus }[] = [
  { label: 'To do', value: 'TODO' },
  { label: 'In progress', value: 'IN_PROGRESS' },
  { label: 'Done', value: 'DONE' },
  { label: 'Archived', value: 'ARCHIVED' },
]

const dataSource = computed<TaskTableRecord[]>(() =>
  props.tasks.map((task) => ({ ...task, key: task.id })),
)
const pendingStatusTaskIds = computed(() => new Set(props.pendingStatusTaskIds ?? []))

function isStatusPending(taskId: string) {
  return pendingStatusTaskIds.value.has(taskId)
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
    size="middle"
  >
    <template #bodyCell="{ column, record }: { column: TaskTableColumn; record: TaskTableRecord }">
      <template v-if="column.key === 'id'">{{ record.id.slice(0, 8) }}</template>
      <template v-else-if="column.key === 'title'">
        <router-link :to="`/tasks/${record.id}`" class="task-link">{{ record.title }}</router-link>
        <div class="desc">{{ record.description || 'No description' }}</div>
      </template>
      <template v-else-if="column.key === 'status'">
        <a-select
          :value="record.status"
          :options="statusOptions"
          :disabled="isStatusPending(record.id)"
          :loading="isStatusPending(record.id)"
          aria-label="Task status"
          class="status-select"
          @change="(status: TaskStatus) => handleStatusChange(record, status)"
        />
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
