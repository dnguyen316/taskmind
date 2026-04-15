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
      <p>Ant Design Vue + Vee Validate form flow with backend task APIs.</p>
    </header>

    <TaskCreateForm :saving="saving" :on-submit-task="submitTask" />

    <a-card title="Tasks">
      <template #extra>
        <TaskFilters :filters="filters" @refresh="fetchTasks" />
      </template>

      <a-alert
        v-if="errorMessage"
        type="error"
        show-icon
        :message="errorMessage"
        class="space-bottom"
      />

      <a-spin :spinning="loading">
        <a-empty v-if="!loading && visibleTasks.length === 0" description="No tasks found for current filter." />
        <TaskList
          v-else
          :tasks="visibleTasks"
          @change-status="(taskId, status) => changeStatus(taskId, status)"
        />
      </a-spin>
    </a-card>
  </main>
</template>

<style scoped>
.page {
  min-height: 100vh;
  max-width: 1200px;
  margin: 0 auto;
  padding: 24px;
  display: grid;
  gap: 16px;
}

.header h1 {
  margin: 0;
}

.header p {
  margin: 4px 0 0;
  color: #64748b;
}

.space-bottom {
  margin-bottom: 12px;
}
</style>
