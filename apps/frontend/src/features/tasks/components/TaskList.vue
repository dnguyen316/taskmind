<script setup>
import { STATUS_TRANSITIONS } from '../constants/taskConstants'

defineProps({
  tasks: {
    type: Array,
    required: true,
  },
})

const emit = defineEmits(['change-status'])

function formatDate(value) {
  if (!value) {
    return '—'
  }

  try {
    return new Date(value).toLocaleString()
  } catch {
    return value
  }
}
</script>

<template>
  <a-list :data-source="tasks" item-layout="vertical">
    <template #renderItem="{ item }">
      <a-list-item>
        <a-list-item-meta>
          <template #title>
            <a-space>
              <span>{{ item.title }}</span>
              <a-tag color="blue">{{ item.status }}</a-tag>
              <a-tag color="purple">P{{ item.priority }}</a-tag>
            </a-space>
          </template>
          <template #description>
            <a-space direction="vertical" size="small">
              <span>{{ item.description || 'No description' }}</span>
              <a-space>
                <span><strong>Due:</strong> {{ formatDate(item.dueAt) }}</span>
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
            @click="emit('change-status', item.id, transition.value)"
          >
            {{ transition.label }}
          </a-button>
        </template>
      </a-list-item>
    </template>
  </a-list>
</template>
