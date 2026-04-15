<script setup>
import { TASK_STATUS_OPTIONS } from '../constants/taskConstants'

defineProps({
  filters: {
    type: Object,
    required: true,
  },
})

const emit = defineEmits(['refresh'])
</script>

<template>
  <div class="filters">
    <label>
      Status
      <select v-model="filters.status" @change="emit('refresh')">
        <option value="">All</option>
        <option v-for="status in TASK_STATUS_OPTIONS" :key="status" :value="status">{{ status }}</option>
      </select>
    </label>

    <label class="checkbox">
      <input v-model="filters.overdueOnly" type="checkbox" @change="emit('refresh')" />
      Overdue only
    </label>

    <button class="secondary" @click="emit('refresh')">Refresh</button>
  </div>
</template>

<style scoped>
.filters {
  display: grid;
  grid-template-columns: repeat(3, auto);
  gap: 0.75rem;
  align-items: end;
}

label {
  display: grid;
  gap: 0.35rem;
  font-weight: 600;
  font-size: 0.9rem;
}

.checkbox {
  display: flex;
  align-items: center;
  gap: 0.45rem;
}

input,
select,
button {
  font: inherit;
}

select {
  border: 1px solid #cbd5e1;
  border-radius: 8px;
  padding: 0.5rem 0.65rem;
}

button {
  border: none;
  border-radius: 8px;
  padding: 0.5rem 0.9rem;
  cursor: pointer;
}

button.secondary {
  background: #e2e8f0;
  color: #0f172a;
}

@media (max-width: 900px) {
  .filters {
    grid-template-columns: 1fr;
  }
}
</style>
