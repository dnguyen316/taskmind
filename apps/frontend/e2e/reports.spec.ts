import { expect, test } from '@playwright/test'

const emptyReport = {
  range: 'WEEK',
  availableMetrics: [
    'tasksCreated',
    'tasksCompleted',
    'projectsCreated',
    'eventsIngested',
    'completionRate',
    'statusSegments',
    'projectThroughput',
    'assigneeThroughput',
    'assigneeWorkload',
    'teamWorkload',
  ],
  dataFreshness:
    'Analytics rollups reflect Relay projections available in Core; priority segments are coming soon because task priority is not projected yet.',
  kpis: {
    tasksCreated: 0,
    tasksCompleted: 0,
    projectsCreated: 0,
    eventsIngested: 0,
    completionRate: 0,
  },
  deltas: { tasksCreated: 0, tasksCompleted: 0, eventsIngested: 0 },
  sparklines: { tasksCreated: [], tasksCompleted: [], eventsIngested: [] },
  trends: [],
  statusSegments: [],
  prioritySegments: [],
  projectThroughput: [],
  assigneeThroughput: [],
  assigneeWorkload: [],
  teamWorkload: { members: 0, openTasks: 0 },
}

test('reports labels unavailable and empty rollup metrics', async ({ page }) => {
  await page.route('**/v1/reports?**', (route) => route.fulfill({ json: emptyReport }))
  await page.goto('/login')
  await page.getByLabel(/email/i).fill('superadmin@taskmind.local')
  await page.getByLabel(/password/i).fill('1')
  await page.getByRole('button', { name: /sign in|login/i }).click()
  await expect(page).toHaveURL(/onboarding|dashboard|projects|reports/)

  await page.goto('/reports')

  await expect(page.getByText(/priority segments are coming soon/i)).toBeVisible()
  await expect(page.getByText('Coming soon')).toBeVisible()
  await expect(page.getByText('Not enough data yet').first()).toBeVisible()
})
