<script setup>
import { STATUS_TRANSITIONS } from '../constants/taskConstants'

defineProps({
  tasks: {
    type: Array,
    required: true,
  },
})

const emit = defineEmits(['change-status'])

function formatDueDate(value) {
  if (!value) {
    return 'No due date'
  }
  return new Date(value).toLocaleString()
}
</script>

<template>
  <a-list item-layout="vertical" :data-source="tasks">
    <template #renderItem="{ item }">
      <a-list-item>
        <template #actions>
          <a-button
            v-for="action in STATUS_TRANSITIONS"
            :key="`${item.id}-${action.value}`"
            size="small"
            @click="emit('change-status', item.id, action.value)"
          >
            {{ action.label }}
          </a-button>
        </template>

        <a-list-item-meta :description="item.description || 'No description'">
          <template #title>
            <a-space>
              <span>{{ item.title }}</span>
              <a-tag color="blue">{{ item.status }}</a-tag>
            </a-space>
          </template>
        </a-list-item-meta>

        <a-typography-text type="secondary">
          Due: {{ formatDueDate(item.dueAt) }} · Priority {{ item.priority }} · Duration {{ item.durationMinutes }}m
        </a-typography-text>
      </a-list-item>
    </template>
  </a-list>
</template>
