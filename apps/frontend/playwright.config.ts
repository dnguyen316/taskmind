import { defineConfig } from '@playwright/test'

// These defaults exist only in the browser-test process. Vite never loads this file or
// receives these variables, so production bundles cannot acquire an E2E login path.
if (!process.env.CI) {
  process.env.E2E_AUTH_EMAIL ??= 'superadmin@taskmind.local'
  process.env.E2E_AUTH_PASSWORD ??= '1'
  process.env.E2E_AUTH_OTP ??= '1'
}

export default defineConfig({
  testDir: './e2e',
  use: {
    baseURL: process.env.E2E_BASE_URL ?? 'http://localhost:5173',
  },
})
