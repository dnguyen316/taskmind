<script setup>
import { computed, onMounted } from 'vue'
import TaskCreateForm from '../components/TaskCreateForm.vue'
import TaskFilters from '../components/TaskFilters.vue'
import TaskList from '../components/TaskList.vue'
import { useTasks } from '../composables/useTasks'

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

const taskMetrics = computed(() => ({
  total: visibleTasks.value.length,
  overdue: visibleTasks.value.filter((task) => task.dueAt && new Date(task.dueAt) < new Date() && task.status !== 'DONE').length,
  inProgress: visibleTasks.value.filter((task) => task.status === 'IN_PROGRESS').length,
}))

onMounted(fetchTasks)
</script>

<template>
  <main class="workspace-page">
    <section class="hero-card">
      <div>
        <p class="eyebrow">TaskMind Design System</p>
        <h1>AI Task Workspace</h1>
        <p class="hero-copy">
          Plan, prioritize, and execute with calm, structured surfaces designed for rapid decisions.
        </p>
      </div>
      <a-space size="middle" class="hero-stats">
        <a-statistic title="Visible tasks" :value="taskMetrics.total" />
        <a-statistic title="In progress" :value="taskMetrics.inProgress" />
        <a-statistic title="Overdue" :value="taskMetrics.overdue" />
      </a-space>
    </section>

    <TaskCreateForm :saving="saving" :on-submit-task="submitTask" />

    <a-card title="Task board" class="surface-card">
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
.workspace-page {
  min-height: 100vh;
  max-width: 1100px;
  margin: 0 auto;
  padding: 32px 20px 40px;
  display: grid;
  gap: 18px;
}

.hero-card {
  background: linear-gradient(135deg, rgba(37, 99, 235, 0.14), rgba(14, 165, 233, 0.08));
  border: 1px solid rgba(148, 163, 184, 0.35);
  border-radius: 20px;
  padding: 24px;
  display: grid;
  gap: 14px;
}

.eyebrow {
  margin: 0;
  color: #1d4ed8;
  font-size: 12px;
  letter-spacing: 0.1em;
  text-transform: uppercase;
  font-weight: 700;
}

.hero-card h1 {
  margin: 6px 0;
  font-size: clamp(1.6rem, 3.8vw, 2.15rem);
}

.hero-copy {
  margin: 0;
  max-width: 620px;
  color: #334155;
}

.hero-stats {
  flex-wrap: wrap;
}

.surface-card {
  border-radius: 18px;
}

.space-bottom {
  margin-bottom: 12px;
}
</style>
