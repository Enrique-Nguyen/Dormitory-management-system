import { test, expect } from '@playwright/test'

test('trang đăng nhập hiển thị nút đăng nhập', async ({ page }) => {
  await page.goto('/login')
  await expect(page.getByRole('heading', { name: 'Đăng nhập hệ thống' })).toBeVisible()
  await expect(page.getByRole('button', { name: 'Đăng nhập' })).toBeVisible()
})

test('chưa đăng nhập vào cổng quản trị thì bị chuyển về trang đăng nhập', async ({ page }) => {
  await page.goto('/admin/users')
  await expect(page).toHaveURL(/\/login|\/realms\//, { timeout: 15_000 })
})
