/** Khai báo vai trò dùng cho E2E: thư mục test, tiền tố route, biến môi trường đăng nhập. */
export type RoleKey = 'student' | 'staff' | 'technician' | 'admin'

export interface E2eRole {
  key: RoleKey
  /** Tiền tố route của cổng; trùng cổng gốc trong constants/navigation.ts. */
  portal: string
  /** Student và Technician thiết kế mobile-first (360 px). */
  mobile: boolean
}

export const ROLES: E2eRole[] = [
  { key: 'student', portal: '/student', mobile: true },
  { key: 'staff', portal: '/staff', mobile: false },
  { key: 'technician', portal: '/technician', mobile: true },
  { key: 'admin', portal: '/admin', mobile: false },
]

/** File lưu phiên đăng nhập; thư mục .auth được Git bỏ qua. */
export const authFile = (key: RoleKey) => `e2e/.auth/${key}.json`

/** Đọc tài khoản từ môi trường; trả null khi chưa cấu hình để test tự bỏ qua. */
export function credentialsOf(key: RoleKey) {
  const prefix = `E2E_${key.toUpperCase()}`
  const username = process.env[`${prefix}_USERNAME`]
  const password = process.env[`${prefix}_PASSWORD`]
  return username && password ? { username, password } : null
}
