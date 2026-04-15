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
  <ul class="task-list">
    <li v-for="task in tasks" :key="task.id" class="task-item">
      <div>
        <h3>{{ task.title }}</h3>
        <p>{{ task.description || 'No description' }}</p>
        <small>
          Due: {{ formatDueDate(task.dueAt) }} · Priority {{ task.priority }} · Duration {{ task.durationMinutes }}m
        </small>
      </div>

      <div class="task-actions">
        <span class="pill">{{ task.status }}</span>
        <div class="actions-grid">
          <button
            v-for="action in STATUS_TRANSITIONS"
            :key="action.value"
            class="secondary"
            @click="emit('change-status', task.id, action.value)"
          >
            {{ action.label }}
          </button>
        </div>
      </div>
    </li>
  </ul>
</template>

<style scoped>
.task-list {
  margin: 0;
  padding: 0;
  list-style: none;
  display: grid;
  gap: 0.8rem;
}

.task-item {
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  padding: 0.9rem;
  display: flex;
  justify-content: space-between;
  gap: 1rem;
}

.task-item h3 {
  margin: 0;
}

.task-item p {
  margin: 0.2rem 0;
}

.task-actions {
  display: grid;
  justify-items: end;
  gap: 0.5rem;
}

.actions-grid {
  display: grid;
  grid-template-columns: repeat(3, auto);
  gap: 0.4rem;
}

button {
  border: none;
  border-radius: 8px;
  padding: 0.5rem 0.9rem;
  cursor: pointer;
  font: inherit;
}

button.secondary {
  background: #e2e8f0;
  color: #0f172a;
}

.pill {
  background: #dbeafe;
  color: #1e3a8a;
  border-radius: 999px;
  padding: 0.2rem 0.6rem;
  font-size: 0.8rem;
}

@media (max-width: 900px) {
  .task-item {
    flex-direction: column;
    align-items: stretch;
  }

  .task-actions {
    justify-items: start;
  }
}
</style>
