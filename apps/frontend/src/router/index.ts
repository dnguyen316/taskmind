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
import { pinia } from '../stores/pinia'
import { useAuthStore } from '../stores/auth'

const publicMeta = { requiresAuth: false, guestOnly: false } as const
const guestOnlyMeta = { requiresAuth: false, guestOnly: true } as const
const protectedMeta = { requiresAuth: true, guestOnly: false } as const

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    name: 'landing',
    component: LandingPage,
    meta: publicMeta,
  },
  {
    path: '/login',
    name: 'login',
    component: AuthPage,
    meta: guestOnlyMeta,
  },
  {
    path: '/signup',
    name: 'signup',
    component: AuthPage,
    meta: guestOnlyMeta,
  },
  {
    path: '/forgot-password',
    name: 'forgot-password',
    component: AuthPage,
    meta: publicMeta,
  },
  {
    path: '/dashboard',
    name: 'dashboard',
    component: DashboardPage,
    meta: protectedMeta,
  },
  {
    path: '/projects',
    name: 'projects-dashboard',
    component: ProjectsDashboardPage,
    meta: protectedMeta,
  },
  {
    path: '/projects/:id',
    name: 'project-detail',
    component: ProjectDetailPage,
    props: true,
    meta: protectedMeta,
  },

  {
    path: '/inbox',
    name: 'inbox-capture',
    component: InboxCapturePage,
    meta: protectedMeta,
  },
  {
    path: '/calendar',
    name: 'calendar',
    component: CalendarPage,
    meta: protectedMeta,
  },
  {
    path: '/team',
    name: 'team',
    component: TeamPage,
    meta: protectedMeta,
  },
  {
    path: '/reports',
    name: 'reports',
    component: ReportsPage,
    meta: protectedMeta,
  },
  {
    path: '/tasks',
    name: 'tasks',
    component: TasksPage,
    meta: protectedMeta,
  },
  {
    path: '/tasks/:id',
    name: 'task-detail',
    component: TaskDetailPage,
    props: true,
    meta: protectedMeta,
  },
  {
    path: '/workspaces',
    name: 'workspaces',
    redirect: '/dashboard',
    meta: protectedMeta,
  },
  {
    path: '/workspaces/:id',
    name: 'workspace-detail',
    redirect: '/dashboard',
    meta: protectedMeta,
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach(async (to) => {
  const authStore = useAuthStore(pinia)

  try {
    await authStore.ensureInitialized()
  } catch {
    // Expired/invalid sessions are normalized by the auth store and handled below.
  }

  if (to.meta.requiresAuth && !authStore.isAuthenticated) {
    return {
      path: '/login',
      query: { redirect: to.fullPath },
    }
  }

  if (to.meta.guestOnly && authStore.isAuthenticated) {
    return '/dashboard'
  }

  return true
})

export default router
