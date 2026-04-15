<script setup>
import { TASK_STATUS_OPTIONS } from '../constants/taskConstants'

defineProps({
  form: {
    type: Object,
    required: true,
  },
  saving: {
    type: Boolean,
    default: false,
  },
})

const emit = defineEmits(['submit'])
</script>

<template>
  <section class="card">
    <h2>Create task</h2>
    <form class="task-form" @submit.prevent="emit('submit')">
      <label>
        Title
        <input v-model="form.title" type="text" placeholder="Ship onboarding flow" required />
      </label>

      <label>
        Description
        <textarea v-model="form.description" rows="3" placeholder="Add details (optional)" />
      </label>

      <div class="grid two">
        <label>
          Status
          <select v-model="form.status">
            <option v-for="status in TASK_STATUS_OPTIONS" :key="status" :value="status">{{ status }}</option>
          </select>
        </label>

        <label>
          Priority (1-5)
          <input v-model.number="form.priority" min="1" max="5" type="number" />
        </label>
      </div>

      <div class="grid two">
        <label>
          Duration (minutes)
          <input v-model.number="form.durationMinutes" min="5" step="5" type="number" />
        </label>

        <label>
          Due at
          <input v-model="form.dueAt" type="datetime-local" />
        </label>
      </div>

      <button :disabled="saving" type="submit">{{ saving ? 'Saving…' : 'Create task' }}</button>
    </form>
  </section>
</template>

<style scoped>
.card {
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 1rem;
}

.task-form {
  display: grid;
  gap: 0.75rem;
}

.grid.two {
  display: grid;
  gap: 0.75rem;
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

label {
  display: grid;
  gap: 0.35rem;
  font-weight: 600;
  font-size: 0.9rem;
}

input,
textarea,
select,
button {
  font: inherit;
}

input,
textarea,
select {
  border: 1px solid #cbd5e1;
  border-radius: 8px;
  padding: 0.5rem 0.65rem;
}

button {
  border: none;
  border-radius: 8px;
  padding: 0.5rem 0.9rem;
  background: #2563eb;
  color: white;
  cursor: pointer;
}

button:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

@media (max-width: 900px) {
  .grid.two {
    grid-template-columns: 1fr;
  }
}
</style>
