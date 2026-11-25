# Instructions

- Following Playwright test failed.
- Explain why, be concise, respect Playwright best practices.
- Provide a snippet of code with the fix, if possible.

# Test info

- Name: anonymous\login.spec.ts >> trang đăng nhập hiển thị nút đăng nhập
- Location: e2e\anonymous\login.spec.ts:3:1

# Error details

```
Error: expect(locator).toBeVisible() failed

Locator: getByRole('heading', { name: 'Đăng nhập hệ thống' })
Expected: visible
Timeout: 5000ms
Error: element(s) not found

Call log:
  - Expect "toBeVisible" getByRole('heading', { name: 'Đăng nhập hệ thống' }) with timeout 5000ms
  - waiting for getByRole('heading', { name: 'Đăng nhập hệ thống' })

```

# Test source

```ts
  1  | import { test, expect } from '@playwright/test'
  2  | 
  3  | test('trang đăng nhập hiển thị nút đăng nhập', async ({ page }) => {
  4  |   await page.goto('/login')
> 5  |   await expect(page.getByRole('heading', { name: 'Đăng nhập hệ thống' })).toBeVisible()
     |                                                                           ^ Error: expect(locator).toBeVisible() failed
  6  |   await expect(page.getByRole('button', { name: 'Đăng nhập' })).toBeVisible()
  7  | })
  8  | 
  9  | test('chưa đăng nhập vào cổng quản trị thì bị chuyển về trang đăng nhập', async ({ page }) => {
  10 |   await page.goto('/admin/users')
  11 |   await expect(page).toHaveURL(/\/login|\/realms\//, { timeout: 15_000 })
  12 | })
  13 | 
```