<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import '../task-list-views.css'
import AppLayout from '../components/AppLayout.vue'
import TaskFilters from '../components/TaskFilters.vue'
import TaskCreateForm from '../components/TaskCreateForm.vue'
import TaskList from '../components/TaskList.vue'
import TaskKanbanView from '../components/TaskKanbanView.vue'
import TaskBacklogView from '../components/TaskBacklogView.vue'
import { useTasks } from '../composables/useTasks'
import type { TaskSortBy, TaskStatus } from '../types'

const route = useRoute()

const {
  loading,
  saving,
  errorMessage,
  pendingStatusTaskIds,
  visibleTasks,
  filters,
  projects,
  savedViews,
  activeProjectId,
  fetchTasks,
  fetchProjects,
  fetchSavedViews,
  saveCurrentView,
  applySavedView,
  submitTask,
  changeStatus,
} = useTasks()

const viewMode = ref<'list' | 'kanban' | 'backlog'>('list')
const isCreateTaskModalOpen = ref(false)

async function handleCreateTask(payload: Parameters<typeof submitTask>[0]) {
  await submitTask(payload)
  isCreateTaskModalOpen.value = false
}
function applyRouteFilters() {
  const query = route.query

  filters.projectId = typeof query.projectId === 'string' ? query.projectId : undefined
  filters.assigneeId = typeof query.assigneeId === 'string' ? query.assigneeId : undefined
  filters.overdueOnly = query.overdueOnly === 'true'
  filters.blocked = query.blocked === 'true'
  filters.unassigned = query.unassigned === 'true'
  filters.stale = query.stale === 'true'
  filters.sortBy = isTaskSortBy(query.sortBy) ? query.sortBy : undefined
}

function isTaskSortBy(value: unknown): value is TaskSortBy {
  return (
    typeof value === 'string' && ['updatedAt', 'dueAt', 'priority', 'createdAt'].includes(value)
  )
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
  applyRouteFilters()
  await fetchProjects()
  await fetchSavedViews()
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
      <div class="topbar__actions">
        <a-radio-group v-model:value="viewMode" button-style="solid" class="tm-view-switcher">
          <a-radio-button value="list">List</a-radio-button>
          <a-radio-button value="kanban">Kanban</a-radio-button>
          <a-radio-button value="backlog">Backlog</a-radio-button>
        </a-radio-group>
        <a-button class="topbar__action" type="primary" @click="isCreateTaskModalOpen = true"
          >New task</a-button
        >
      </div>
    </header>

    <section class="tasks-content">
      <TaskFilters
        :filters="filters"
        :projects="projects"
        :saved-views="savedViews"
        @refresh="fetchTasks"
        @apply-saved-view="
          (view) => {
            applySavedView(view)
            fetchTasks()
          }
        "
        @save-view="saveCurrentView"
      />
      <a-alert
        v-if="errorMessage"
        type="error"
        show-icon
        :message="errorMessage"
        class="tasks-error"
      />
      <TaskList
        v-if="viewMode === 'list'"
        :tasks="visibleTasks"
        :pending-status-task-ids="pendingStatusTaskIds"
        :loading="loading"
        :error-message="errorMessage"
        @change-status="handleChangeStatus"
      />
      <TaskKanbanView
        v-else-if="viewMode === 'kanban'"
        :tasks="visibleTasks"
        :pending-status-task-ids="pendingStatusTaskIds"
        :loading="loading"
        @change-status="handleChangeStatus"
      />
      <TaskBacklogView
        v-else
        :tasks="visibleTasks"
        :pending-status-task-ids="pendingStatusTaskIds"
        :loading="loading"
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
  margin-bottom: 24px;
}

.topbar h1 {
  margin: 0;
  font-size: 32px;
}

.topbar p {
  margin: 4px 0 0;
  color: #64748b;
}

.topbar__actions {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.topbar__action {
  flex-shrink: 0;
}

.tasks-content {
  display: flex;
  width: 100%;
  flex-direction: column;
  gap: 16px;
}

.tasks-error {
  margin: 0;
}

@media (max-width: 575px) {
  .topbar {
    flex-direction: column;
    align-items: stretch;
  }

  .topbar__actions,
  .topbar__action {
    width: 100%;
  }

  .topbar__actions {
    justify-content: stretch;
  }
}
</style>
