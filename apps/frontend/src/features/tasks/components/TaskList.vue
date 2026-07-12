<script setup lang="ts">
import { computed } from 'vue'
import TaskStatusChip from './TaskStatusChip.vue'
import { TASK_STATUS_SELECT_OPTIONS } from '../constants/taskPresentation'
import { formatDateTime, isTaskOverdue, toTimestamp } from '../utils/taskDates'
import { taskDetailRoute as buildTaskDetailRoute } from '../utils/taskRoutes'
import { taskHealth, taskHealthColor, taskHealthLabel } from '../utils/taskHealth'
import type { Task, TaskStatus } from '../types'

const props = defineProps<{
  tasks: Task[]
  pendingStatusTaskIds?: string[]
  loading?: boolean
  errorMessage?: string
  projectId?: string
}>()
const emit = defineEmits<{
  changeStatus: [taskId: string, status: TaskStatus, version: number | null]
}>()

interface TaskTableColumn {
  title: string
  dataIndex: keyof Task | 'key' | 'health'
  key: keyof Task | 'key' | 'health'
  width?: number
  ellipsis?: boolean
}

interface TaskTableRecord extends Task {
  key: string
}

const columns: TaskTableColumn[] = [
  { title: 'HEALTH', dataIndex: 'status', key: 'health', width: 110 },
  { title: 'TASK', dataIndex: 'title', key: 'title', width: 360, ellipsis: true },
  { title: 'PROJECT', dataIndex: 'projectId', key: 'projectId', width: 180, ellipsis: true },
  { title: 'OWNER', dataIndex: 'userId', key: 'userId', width: 180, ellipsis: true },
  { title: 'STATUS', dataIndex: 'status', key: 'status', width: 180 },
  { title: 'PRIORITY', dataIndex: 'priority', key: 'priority', width: 110 },
  { title: 'DUE', dataIndex: 'dueAt', key: 'dueAt', width: 160 },
  { title: 'UPDATED', dataIndex: 'updatedAt', key: 'updatedAt', width: 170 },
  { title: 'CREATED', dataIndex: 'createdAt', key: 'createdAt', width: 170 },
  { title: 'EFFORT', dataIndex: 'durationMinutes', key: 'durationMinutes', width: 90 },
]

const tableScroll = { x: 1400 }

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
    emit('changeStatus', task.id, nextStatus, task.version)
  }
}

function compactId(value: string) {
  return value.slice(0, 8)
}

function taskKeyLabel(task: Task) {
  return task.taskKey || compactId(task.id)
}

function ownerLabel(task: Task) {
  return task.assigneeId || task.userId
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
    :scroll="tableScroll"
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
      <template v-if="column.key === 'health'">
        <a-tag :color="taskHealthColor(taskHealth(record))">{{
          taskHealthLabel(taskHealth(record))
        }}</a-tag>
      </template>
      <template v-else-if="column.key === 'title'">
        <div class="task-title-cell">
          <router-link :to="taskDetailRoute(record)" class="task-link" :title="record.title">
            {{ record.title }}
          </router-link>
          <span class="task-key">{{ taskKeyLabel(record) }}</span>
        </div>
        <div class="desc" :title="record.description || 'No description'">
          {{ record.description || 'No description' }}
        </div>
      </template>
      <template v-else-if="column.key === 'projectId'">
        <span class="secondary-id" :title="record.projectId">{{
          compactId(record.projectId)
        }}</span>
      </template>
      <template v-else-if="column.key === 'userId'">
        <span class="secondary-id" :title="ownerLabel(record)">{{
          compactId(ownerLabel(record))
        }}</span>
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
      <template v-else-if="column.key === 'updatedAt'">{{
        formatDateTime(record.updatedAt)
      }}</template>
      <template v-else-if="column.key === 'createdAt'">{{
        formatDateTime(record.createdAt)
      }}</template>
      <template v-else-if="column.key === 'durationMinutes'">{{
        Math.max(1, Math.round((record.durationMinutes || 30) / 30))
      }}</template>
    </template>
  </a-table>
</template>

<style scoped>
.task-title-cell {
  align-items: center;
  display: flex;
  gap: 8px;
  min-width: 0;
}
.task-link {
  display: block;
  flex: 1 1 auto;
  font-weight: 600;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.task-key {
  background: #f1f5f9;
  border-radius: 999px;
  color: #475569;
  flex: 0 0 auto;
  font-size: 11px;
  font-weight: 600;
  line-height: 1;
  padding: 4px 8px;
}
.desc {
  color: #64748b;
  font-size: 12px;
  margin-top: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.secondary-id {
  color: #64748b;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, 'Liberation Mono', monospace;
  font-size: 12px;
}
.overdue {
  color: #cf1322;
  font-weight: 600;
}
.status-select {
  width: 140px;
}
</style>
