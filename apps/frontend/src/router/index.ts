import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
import DashboardPage from '../features/tasks/pages/DashboardPage.vue'
import TasksPage from '../features/tasks/pages/TasksPage.vue'
import TaskDetailPage from '../features/tasks/pages/TaskDetailPage.vue'
import ProjectsDashboardPage from '../features/projects/pages/ProjectsDashboardPage.vue'
import ProjectDetailPage from '../features/projects/pages/ProjectDetailPage.vue'
import LandingPage from '../features/landing/pages/LandingPage.vue'
import AuthPage from '../features/auth/pages/AuthPage.vue'
import InboxCapturePage from '../features/ai/pages/InboxCapturePage.vue'
import CalendarPage from '../features/scheduler/pages/CalendarPage.vue'
import TeamPage from '../features/team/pages/TeamPage.vue'
import ReportsPage from '../features/reports/pages/ReportsPage.vue'

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    name: 'landing',
    component: LandingPage,
  },
  {
    path: '/login',
    name: 'login',
    component: AuthPage,
  },
  {
    path: '/signup',
    name: 'signup',
    component: AuthPage,
  },
  {
    path: '/dashboard',
    name: 'dashboard',
    component: DashboardPage,
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
    path: '/inbox',
    name: 'inbox-capture',
    component: InboxCapturePage,
  },
  {
    path: '/calendar',
    name: 'calendar',
    component: CalendarPage,
  },
  {
    path: '/team',
    name: 'team',
    component: TeamPage,
  },
  {
    path: '/reports',
    name: 'reports',
    component: ReportsPage,
  },
  {
    path: '/tasks',
    name: 'tasks',
    component: TasksPage,
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
