# E2E (Playwright)

Chạy trên hệ thống thật, không mock. Cần sẵn: Postgres, Keycloak, backend :8080
(`scripts/start-local.ps1 -ProvisionAccounts` hoặc `docker compose up -d` + `mvnw spring-boot:run`).

## Tài khoản test

Đặt biến môi trường (không commit); tài khoản phải **đã đổi mật khẩu lần đầu**:

```powershell
$env:E2E_STUDENT_USERNAME="..."; $env:E2E_STUDENT_PASSWORD="..."
# tương tự E2E_STAFF_*, E2E_TECHNICIAN_*, E2E_ADMIN_*
```

Vai trò thiếu biến thì test của vai trò đó tự skip.

## Lệnh

```powershell
npm run e2e          # chạy tất cả
npm run e2e:ui       # giao diện debug
npx playwright test e2e/admin
npx playwright show-report
```

`E2E_BASE_URL` trỏ tới môi trường đã deploy (khi đó không tự bật Vite).
Thêm test mới vào `e2e/<role>/*.spec.ts`; project theo thư mục được khai trong `playwright.config.ts`.
