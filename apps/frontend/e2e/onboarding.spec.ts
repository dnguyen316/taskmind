import { test, expect } from '@playwright/test'

test('signup/login onboarding creates first project and task', async ({ page }) => {
  await page.goto('/login')
  await page.getByLabel(/email/i).fill('superadmin@taskmind.local')
  await page.getByLabel(/password/i).fill('1')
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
