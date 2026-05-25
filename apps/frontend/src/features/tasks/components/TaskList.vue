<script setup lang="ts">
import { STATUS_TRANSITIONS } from '../constants/taskConstants'
import { formatDateTime } from '../utils/taskDates'
import type { Task, TaskStatus } from '../types'

const props = defineProps<{
  tasks: Task[]
}>()

const emit = defineEmits<{
  changeStatus: [taskId: string, status: TaskStatus]
}>()

function statusColor(status: TaskStatus) {
  return {
    TODO: 'default',
    IN_PROGRESS: 'processing',
    DONE: 'success',
    ARCHIVED: 'purple',
  }[status] ?? 'default'
}
</script>

<template>
  <a-list :data-source="props.tasks" item-layout="vertical" class="task-list">
    <template #renderItem="{ item }">
      <a-list-item class="task-item">
        <a-list-item-meta>
          <template #title>
            <a-space>
              <router-link :to="`/tasks/${item.id}`" class="task-title-link">
                <span class="task-title">{{ item.title }}</span>
              </router-link>
              <a-tag :color="statusColor(item.status)">{{ item.status }}</a-tag>
              <a-tag color="blue">P{{ item.priority }}</a-tag>
            </a-space>
          </template>
          <template #description>
            <a-space direction="vertical" size="small">
              <span>{{ item.description || 'No description' }}</span>
              <a-space>
                <span><strong>Due:</strong> {{ formatDateTime(item.dueAt) }}</span>
                <span><strong>Duration:</strong> {{ item.durationMinutes ?? '—' }} min</span>
              </a-space>
            </a-space>
          </template>
        </a-list-item-meta>

        <template #actions>
          <a-button
            v-for="transition in STATUS_TRANSITIONS"
            :key="`${item.id}-${transition.value}`"
            size="small"
            :disabled="item.status === transition.value"
            @click="emit('changeStatus', item.id, transition.value)"
          >
            {{ transition.label }}
          </a-button>
        </template>
      </a-list-item>
    </template>
  </a-list>
</template>

<style scoped>
.task-list {
  border-radius: 14px;
}

.task-item {
  border: 1px solid #e2e8f0;
  border-radius: 14px;
  margin-bottom: 10px;
  padding: 12px 16px;
  background: #f8fafc;
}

.task-title-link {
  color: #1677ff;
}

.task-title {
  font-weight: 600;
}
</style>
