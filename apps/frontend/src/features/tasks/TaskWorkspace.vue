<script setup>
import { onMounted } from 'vue'
import TaskCreateForm from './components/TaskCreateForm.vue'
import TaskFilters from './components/TaskFilters.vue'
import TaskList from './components/TaskList.vue'
import { useTasks } from './composables/useTasks'

const {
  loading,
  saving,
  errorMessage,
  filters,
  createForm,
  visibleTasks,
  fetchTasks,
  submitTask,
  changeStatus,
} = useTasks()

onMounted(fetchTasks)
</script>

<template>
  <main class="page">
    <header class="header">
      <h1>TaskMind · FE Task Workspace</h1>
      <p>Create tasks, filter them, and quickly update status against the backend API.</p>
    </header>

    <TaskCreateForm :form="createForm" :saving="saving" @submit="submitTask" />

    <section class="card">
      <div class="row">
        <h2>Tasks</h2>
        <TaskFilters :filters="filters" @refresh="fetchTasks" />
      </div>

      <p v-if="errorMessage" class="error">{{ errorMessage }}</p>
      <p v-if="loading">Loading tasks…</p>
      <p v-else-if="visibleTasks.length === 0">No tasks found for the current filter.</p>

      <TaskList
        v-else
        :tasks="visibleTasks"
        @change-status="(taskId, status) => changeStatus(taskId, status)"
      />
    </section>
  </main>
</template>

<style scoped>
.page {
  min-height: 100vh;
  background: #f8fafc;
  font-family: Inter, system-ui, -apple-system, Segoe UI, Roboto, sans-serif;
  color: #0f172a;
  padding: 2rem;
  display: grid;
  gap: 1rem;
}

.header h1 {
  margin: 0;
}

.header p {
  margin: 0.25rem 0 0;
  color: #334155;
}

.card {
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 1rem;
}

.row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 1rem;
}

.error {
  color: #b91c1c;
  font-weight: 600;
}

@media (max-width: 900px) {
  .row {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
