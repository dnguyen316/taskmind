<script setup lang="ts">
import { onMounted, ref } from 'vue'
import AppLayout from '../components/AppLayout.vue'
import TaskFilters from '../components/TaskFilters.vue'
import TaskCreateForm from '../components/TaskCreateForm.vue'
import TaskList from '../components/TaskList.vue'
import { useTasks } from '../composables/useTasks'
import type { TaskStatus } from '../types'

const {
  loading,
  saving,
  errorMessage,
  pendingStatusTaskIds,
  visibleTasks,
  filters,
  projects,
  activeProjectId,
  fetchTasks,
  fetchProjects,
  submitTask,
  changeStatus,
} = useTasks()

const isCreateTaskModalOpen = ref(false)

async function handleCreateTask(payload: Parameters<typeof submitTask>[0]) {
  await submitTask(payload)
  isCreateTaskModalOpen.value = false
}
function handleCreateTaskModalCancel() {
  if (!saving.value) {
    isCreateTaskModalOpen.value = false
  }
}
async function handleChangeStatus(taskId: string, status: TaskStatus) {
  await changeStatus(taskId, status)
}

onMounted(async () => {
  await fetchProjects()
  await fetchTasks()
})
</script>

<template>
  <AppLayout :task-count="visibleTasks.length">
    <header class="topbar">
      <div>
        <h1>Tasks</h1>
        <p>Everything assigned, in progress, or waiting</p>
      </div>
      <a-button type="primary" @click="isCreateTaskModalOpen = true">New task</a-button>
    </header>

    <section class="tasks-content">
      <TaskFilters :filters="filters" :projects="projects" @refresh="fetchTasks" />
      <a-alert
        v-if="errorMessage"
        type="error"
        show-icon
        :message="errorMessage"
        class="tasks-error"
      />
      <TaskList
        :tasks="visibleTasks"
        :pending-status-task-ids="pendingStatusTaskIds"
        :loading="loading"
        :error-message="errorMessage"
        @change-status="handleChangeStatus"
      />
    </section>

    <a-modal
      v-model:open="isCreateTaskModalOpen"
      title="New task"
      :footer="null"
      :closable="!saving"
      :keyboard="!saving"
      :mask-closable="!saving"
      @cancel="handleCreateTaskModalCancel"
    >
      <TaskCreateForm
        :project-options="projects"
        :default-project-id="activeProjectId"
        :saving="saving"
        :on-submit-task="handleCreateTask"
      />
    </a-modal>
  </AppLayout>
</template>

<style scoped>
.topbar {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
}
.topbar h1 {
  margin: 0;
  font-size: 32px;
}
.topbar p {
  margin: 4px 0 0;
  color: #64748b;
}
.tasks-content {
  width: 100%;
}
.tasks-error {
  margin-bottom: 12px;
}
</style>
