import { expect } from '@playwright/test'
import type { Page } from '@playwright/test'

const taskId = '11111111-1111-4111-8111-111111111111'
const projectId = '22222222-2222-4222-8222-222222222222'

// Exercise the real routed UI against deterministic Core responses, without an AI or DB dependency.
export async function openEditor(
  page: Page,
  description = '## Launch checklist\n\nKeep the team aligned on the next release.\n\n- [ ] Confirm launch messaging\n- [x] Review the rollout plan\n\n- Share the release notes',
) {
  const state = {
    failSave: false,
    patches: [] as Record<string, unknown>[],
    task: {
      id: taskId,
      projectId,
      userId: taskId,
      version: 1,
      taskKey: 'TM-42',
      taskLevel: 'TASK',
      taskType: 'TASK',
      title: 'Plan the product launch',
      description,
      status: 'IN_PROGRESS',
      priority: 2,
      dueAt: '2026-10-23T09:00:00Z',
      durationMinutes: 45,
      energyLevel: 'MEDIUM',
      source: 'MANUAL',
      createdAt: '2026-10-01T00:00:00Z',
      updatedAt: '2026-10-01T00:00:00Z',
    },
  }
  await page.route('**/v1/**', async (route) => {
    const path = new URL(route.request().url()).pathname
    let body: unknown = []
    if (path.endsWith('/auth/token/refresh'))
      body = { accessToken: 'test-session', tokenType: 'Bearer', expiresInSeconds: 3600 }
    else if (path.endsWith('/auth/me'))
      body = {
        userId: taskId,
        email: 'editor@test.local',
        displayName: 'Editor',
        onboardingCompleted: true,
      }
    else if (path.endsWith('/task-types'))
      body = [
        {
          id: 'type-task',
          key: 'TASK',
          name: 'Task',
          active: true,
          system: true,
          defaultTaskLevel: 'TASK',
          allowedTaskLevels: ['TASK'],
          createdAt: '2026-10-01',
          updatedAt: '2026-10-01',
        },
      ]
    else if (path === `/v1/tasks/${taskId}`) {
      if (route.request().method() === 'PATCH') {
        state.patches.push(route.request().postDataJSON())
        if (state.failSave) {
          await route.fulfill({
            status: 409,
            json: { message: 'This task changed elsewhere. Reload before saving.' },
          })
          return
        }
        state.task = {
          ...state.task,
          ...route.request().postDataJSON(),
          version: state.task.version + 1,
        }
      }
      body = state.task
    }
    await route.fulfill({ json: body })
  })
  await page.goto(`/tasks/${taskId}`)
  await expect(page.getByRole('textbox', { name: 'Task description', exact: true })).toBeVisible()
  await expect(page.getByLabel('Task title', { exact: true })).toHaveValue(
    'Plan the product launch',
  )
  return state
}
