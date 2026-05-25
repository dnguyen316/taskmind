<script setup lang="ts">
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { LeftOutlined } from '@ant-design/icons-vue'
import TaskFilters from '../components/TaskFilters.vue'
import TaskCreateForm from '../components/TaskCreateForm.vue'
import TaskList from '../components/TaskList.vue'
import { useTasks } from '../composables/useTasks'
import type { TaskStatus } from '../types'

const router = useRouter()
const { loading, visibleTasks, filters, projects, fetchTasks, fetchProjects, submitTask, changeStatus } = useTasks()

async function handleCreateTask(payload: Parameters<typeof submitTask>[0]) { await submitTask(payload) }
async function handleChangeStatus(taskId: string, status: TaskStatus) { await changeStatus(taskId, status) }

onMounted(async () => { await fetchProjects(); await fetchTasks() })
</script>

<template>
  <main class="tasks-page">
    <section class="tasks-shell">
      <header class="tasks-header">
        <a-button type="text" @click="router.push('/dashboard')"><LeftOutlined /> Dashboard</a-button>
        <div>
          <h1>Tasks</h1>
          <p>Everything assigned, in progress, or waiting</p>
        </div>
      </header>

      <a-row :gutter="16">
        <a-col :xs="24" :xl="17">
          <TaskFilters :filters="filters" @refresh="fetchTasks" />
          <TaskList :tasks="visibleTasks" @change-status="handleChangeStatus" />
        </a-col>
        <a-col :xs="24" :xl="7">
          <TaskCreateForm :project-options="projects" :saving="loading" :on-submit-task="handleCreateTask" />
        </a-col>
      </a-row>
    </section>
  </main>
</template>

<style scoped>
.tasks-page { min-height: 100vh; background: #f5f7fb; padding: 20px; }
.tasks-shell { max-width: 1400px; margin: 0 auto; }
.tasks-header { display: flex; align-items: center; gap: 12px; margin-bottom: 14px; }
.tasks-header h1 { margin: 0; font-size: 28px; }
.tasks-header p { margin: 0; color: #64748b; }
</style>
