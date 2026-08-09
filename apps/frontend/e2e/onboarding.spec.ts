import { test, expect } from '@playwright/test'
import { e2eAuthCredentials } from './support/auth'

test('signup/login onboarding creates first project and task', async ({ page }) => {
  await page.goto('/login')
  const credentials = e2eAuthCredentials()
  await page.getByLabel(/email/i).fill(credentials.email)
  await page.getByLabel(/password/i).fill(credentials.password)
  await page.getByRole('button', { name: /sign in|login/i }).click()
  await expect(page).toHaveURL(/onboarding|dashboard|projects/)
  if (page.url().includes('/onboarding')) {
    await page.getByRole('radio', { name: /team/i }).check()
    await page.getByRole('button', { name: /continue/i }).click()
    await page.getByRole('radio', { name: /sprint/i }).check()
    await page.getByRole('button', { name: /continue/i }).click()
    await page.getByRole('radio', { name: /demo/i }).check()
    await page.getByRole('button', { name: /create workspace/i }).click()
  }
  await expect(page).toHaveURL(/projects|dashboard/)
  await page.goto('/projects')
  await expect(
    page.getByText(/TaskMind Demo Project|Product launch|My TaskMind Workspace/),
  ).toBeVisible()
})
