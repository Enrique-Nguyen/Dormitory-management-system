import { mkdirSync, writeFileSync } from 'node:fs'
import { test as setup, expect } from '@playwright/test'
import { ROLES, authFile, credentialsOf } from './roles'

/**
 * Đăng nhập một lần cho mỗi vai trò qua trang Keycloak thật rồi lưu storageState.
 * Thiếu biến môi trường thì ghi phiên rỗng và để các test của vai trò đó tự skip.
 */
for (const role of ROLES) {
  setup(`đăng nhập ${role.key}`, async ({ page }) => {
    mkdirSync('e2e/.auth', { recursive: true })
    const credentials = credentialsOf(role.key)
    if (!credentials) {
      writeFileSync(authFile(role.key), JSON.stringify({ cookies: [], origins: [] }))
      setup.skip(true, `Chưa đặt E2E_${role.key.toUpperCase()}_USERNAME/PASSWORD`)
      return
    }

    await page.goto('/login')
    await page.getByRole('button', { name: 'Đăng nhập' }).click()
    await page.locator('#username').fill(credentials.username)
    await page.locator('#password').fill(credentials.password)
    await page.locator('#kc-login').click()

    // Tài khoản phải đã đổi mật khẩu lần đầu, nếu không sẽ bị đưa sang trang đổi mật khẩu.
    await expect(page).toHaveURL(new RegExp(`${role.portal}(/|$)`), { timeout: 30_000 })
    await page.context().storageState({ path: authFile(role.key) })
  })
}
