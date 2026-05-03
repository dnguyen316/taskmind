import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
import TasksDashboardPage from '../features/tasks/pages/TasksDashboardPage.vue'
import TaskDetailPage from '../features/tasks/pages/TaskDetailPage.vue'
import ProjectsDashboardPage from '../features/projects/pages/ProjectsDashboardPage.vue'
import ProjectDetailPage from '../features/projects/pages/ProjectDetailPage.vue'

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    redirect: { name: 'projects-dashboard' },
  },
  {
    path: '/projects',
    name: 'projects-dashboard',
    component: ProjectsDashboardPage,
  },
  {
    path: '/projects/:id',
    name: 'project-detail',
    component: ProjectDetailPage,
    props: true,
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
