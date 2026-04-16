import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
import TasksDashboardPage from '../features/tasks/pages/TasksDashboardPage.vue'
import TaskDetailPage from '../features/tasks/pages/TaskDetailPage.vue'

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    redirect: { name: 'tasks-dashboard' },
  },
  {
    path: '/tasks',
    name: 'tasks-dashboard',
    component: TasksDashboardPage,
  },
  {
    path: '/tasks/:id',
    name: 'task-detail',
    component: TaskDetailPage,
    props: true,
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

export default router
