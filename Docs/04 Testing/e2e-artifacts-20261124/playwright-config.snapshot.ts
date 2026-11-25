import { defineConfig, devices } from '@playwright/test'
import { ROLES, authFile } from './e2e/roles'

/**
 * E2E chạy trên hệ thống thật: Postgres + Keycloak + backend :8080 phải đã chạy
 * (xem e2e/README.md). Frontend được Playwright tự khởi động bằng Vite.
 * Thông tin đăng nhập lấy từ biến môi trường E2E_<ROLE>_USERNAME / E2E_<ROLE>_PASSWORD.
 */
const baseURL = process.env.E2E_BASE_URL ?? 'http://localhost:5173'

export default defineConfig({
  testDir: './e2e',
  fullyParallel: true,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 1 : 0,
  reporter: process.env.CI ? [['github'], ['html', { open: 'never' }]] : 'list',
  use: {
    baseURL,
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
    video: 'retain-on-failure',
    locale: 'vi-VN',
    timezoneId: 'Asia/Ho_Chi_Minh',
  },
  webServer: process.env.E2E_BASE_URL
    ? undefined
    : {
        command: 'npm run dev',
        url: baseURL,
        reuseExistingServer: !process.env.CI,
        timeout: 120_000,
      },
  projects: [
    { name: 'setup', testMatch: /auth\.setup\.ts/ },
    { name: 'anonymous', testMatch: /anonymous\/.*\.spec\.ts/ },
    ...ROLES.map((role) => ({
      name: role.key,
      testMatch: new RegExp(`${role.key}/.*[.]spec[.]ts`),
      dependencies: ['setup'],
      use: {
        ...(role.mobile ? devices['Pixel 5'] : devices['Desktop Chrome']),
        storageState: authFile(role.key),
      },
    })),
  ],
})
