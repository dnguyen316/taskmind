import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
import DashboardPage from '../features/tasks/pages/DashboardPage.vue'
import TasksPage from '../features/tasks/pages/TasksPage.vue'
import TaskDetailPage from '../features/tasks/pages/TaskDetailPage.vue'
import ProjectsDashboardPage from '../features/projects/pages/ProjectsDashboardPage.vue'
import ProjectDetailPage from '../features/projects/pages/ProjectDetailPage.vue'
import LandingPage from '../features/landing/pages/LandingPage.vue'
import AuthPage from '../features/auth/pages/AuthPage.vue'
import ForgotPasswordPage from '../features/auth/pages/ForgotPasswordPage.vue'
import InboxCapturePage from '../features/ai/pages/InboxCapturePage.vue'
import CalendarPage from '../features/scheduler/pages/CalendarPage.vue'
import TeamPage from '../features/team/pages/TeamPage.vue'
import ReportsPage from '../features/reports/pages/ReportsPage.vue'
import ActivitySearchPage from '../features/activity/pages/ActivitySearchPage.vue'
import AiWorkflowTemplatesPage from '../features/aiWorkflows/pages/AiWorkflowTemplatesPage.vue'
import SpecBreakdownPage from '../features/specbreakdown/pages/SpecBreakdownPage.vue'
import OnboardingPage from '../features/onboarding/pages/OnboardingPage.vue'
import { listProjectMembers } from '../features/projects/api/projectsApi'
import { pinia } from '../stores/pinia'
import { useAuthStore } from '../stores/auth'

const publicMeta = { requiresAuth: false, guestOnly: false } as const
const guestOnlyMeta = { requiresAuth: false, guestOnly: true } as const
const protectedMeta = { requiresAuth: true, guestOnly: false } as const
const onboardingMeta = {
  requiresAuth: true,
  guestOnly: false,
  allowsIncompleteOnboarding: true,
} as const
const projectAdminMeta = {
  requiresAuth: true,
  guestOnly: false,
  requiresProjectAdmin: true,
} as const

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
    component: ForgotPasswordPage,
    meta: publicMeta,
  },
  {
    path: '/onboarding',
    name: 'onboarding',
    component: OnboardingPage,
    meta: onboardingMeta,
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
    path: '/projects/:projectId/tasks/:taskId',
    name: 'project-task-detail',
    component: TaskDetailPage,
    props: true,
    meta: protectedMeta,
  },
  {
    path: '/projects/:projectId/ai-workflows',
    name: 'project-ai-workflows',
    component: AiWorkflowTemplatesPage,
    props: true,
    meta: projectAdminMeta,
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
    path: '/activity',
    name: 'activity-search',
    component: ActivitySearchPage,
    meta: protectedMeta,
  },
  {
    path: '/spec-breakdown',
    name: 'spec-breakdown',
    component: SpecBreakdownPage,
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

  if (
    authStore.isAuthenticated &&
    !authStore.currentUser?.onboardingCompleted &&
    !to.meta.allowsIncompleteOnboarding
  ) {
    return { name: 'onboarding', query: { redirect: to.fullPath } }
  }

  if (
    authStore.isAuthenticated &&
    authStore.currentUser?.onboardingCompleted &&
    to.name === 'onboarding'
  ) {
    return typeof to.query.redirect === 'string' ? to.query.redirect : '/dashboard'
  }

  if (to.meta.requiresProjectAdmin === true) {
    const projectId = String(to.params.projectId ?? to.params.id ?? '')
    const members = await listProjectMembers(projectId)
    const currentMembership = members.find((member) => member.userId === authStore.currentUserId)
    if (!currentMembership || !['OWNER', 'ADMIN'].includes(currentMembership.role)) {
      return { name: 'project-detail', params: { id: projectId } }
    }
  }

  if (to.meta.guestOnly && authStore.isAuthenticated) {
    return authStore.currentUser?.onboardingCompleted ? '/dashboard' : '/onboarding'
  }

  return true
})

export default router
