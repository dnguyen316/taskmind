<script setup lang="ts">
import { onMounted } from 'vue'
import AppLayout from '../components/AppLayout.vue'
import TaskFilters from '../components/TaskFilters.vue'
import TaskCreateForm from '../components/TaskCreateForm.vue'
import TaskList from '../components/TaskList.vue'
import { useTasks } from '../composables/useTasks'
import type { TaskStatus } from '../types'

const { loading, visibleTasks, filters, projects, fetchTasks, fetchProjects, submitTask, changeStatus } = useTasks()

async function handleCreateTask(payload: Parameters<typeof submitTask>[0]) { await submitTask(payload) }
async function handleChangeStatus(taskId: string, status: TaskStatus) { await changeStatus(taskId, status) }

onMounted(async () => { await fetchProjects(); await fetchTasks() })
</script>

<template>
  <AppLayout :task-count="visibleTasks.length">
      <header class="topbar">
        <div>
          <h1>Tasks</h1>
          <p>Everything assigned, in progress, or waiting</p>
        </div>
      </header>

      <a-row :gutter="16" align="top">
        <a-col :xs="24" :xl="16">
          <TaskFilters :filters="filters" @refresh="fetchTasks" />
          <TaskList :tasks="visibleTasks" @change-status="handleChangeStatus" />
        </a-col>
        <a-col :xs="24" :xl="8">
          <TaskCreateForm :project-options="projects" :saving="loading" :on-submit-task="handleCreateTask" />
        </a-col>
      </a-row>
    </AppLayout>
</template>

<style scoped>
.topbar h1 { margin:0; font-size:32px; }
.topbar p { margin:4px 0 0; color:#64748b; }
</style>
