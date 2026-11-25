import { test, expect } from '@playwright/test'
import { ROLES, credentialsOf } from '../roles'

const role = ROLES.find((item) => item.key === 'staff')!

test.skip(!credentialsOf(role.key), 'Chưa cấu hình tài khoản E2E cho vai trò này')

test('vào đúng cổng staff sau khi đăng nhập', async ({ page }) => {
  await page.goto(role.portal)
  await expect(page).toHaveURL(new RegExp(`${role.portal}(/|$)`))
})

test('vào cổng của vai trò khác thì bị chuyển về cổng của mình', async ({ page }) => {
  const other = role.key === 'admin' ? '/student' : '/admin'
  await page.goto(other)
  await expect(page).toHaveURL(new RegExp(`${role.portal}(/|$)`))
})
