import { Permission, Role } from '../../constants/authorization'
import axios from 'axios'
import Keycloak from 'keycloak-js'
import { queryClient } from '../../app/queryClient'
import {
  CHANGE_PASSWORD_PATH,
  permissionsForPath,
  roleHome,
  rolePortal,
} from '../../constants/navigation'
import type { Me } from '../../types/auth'
import { api } from './client'

/** Public client: tuyệt đối không đưa secret quản trị vào biến VITE_* hoặc trình duyệt. */
export const keycloak = new Keycloak({
  url: import.meta.env.VITE_KEYCLOAK_URL || 'http://localhost:8180',
  realm: import.meta.env.VITE_KEYCLOAK_REALM || 'dms',
  clientId: import.meta.env.VITE_KEYCLOAK_CLIENT_ID || 'dms-web',
})

/** Khởi tạo một lần trước React Router; token chỉ giữ trong bộ nhớ của keycloak-js. */
export async function initializeAuthentication(): Promise<void> {
  await keycloak.init({
    onLoad: 'check-sso',
    pkceMethod: 'S256',
    checkLoginIframe: false,
  })
}

/** Chỉ nhận đường dẫn nội bộ để không biến redirect thành liên kết tới website khác. */
export function authenticationRedirect(path = '/login'): string {
  const safe =
    path.startsWith('/') && !path.startsWith('//') && !path.includes('\\')
      ? path
      : '/login'
  return new URL(safe, window.location.origin).href
}

export async function loginWithKeycloak(path = '/login'): Promise<void> {
  await keycloak.login({ redirectUri: authenticationRedirect(path) })
}

export async function changeKeycloakPassword(): Promise<void> {
  // Application initiated action: Keycloak tự xác minh và đổi mật khẩu.
  await keycloak.login({
    action: 'UPDATE_PASSWORD',
    redirectUri: authenticationRedirect('/login'),
  })
}

/** Đọc hồ sơ và phạm vi DMS từ danh tính đã được Spring Security xác thực. */
export async function getMe(): Promise<Me> {
  return (await api.get<Me>('/auth/me')).data
}

/** Kiểm tra quyền do máy chủ cấp, dùng chung cho menu, màn hình và từng nút CRUD. */
export function hasPermission(
  me: Me | null | undefined,
  permission: string | undefined,
): boolean {
  return (
    typeof permission === 'string' &&
    Object.hasOwn(Permission, permission) &&
    (me?.permissions?.includes(permission) ?? false)
  )
}

/** Một mục ghép nhiều màn được mở khi tài khoản có quyền đọc ít nhất một màn con. */
export function hasAnyPermission(
  me: Me | null | undefined,
  permissions: string[],
): boolean {
  return permissions.some((permission) => hasPermission(me, permission))
}

/** Thu hồi token tại DMS, dọn cache các tab rồi kết thúc phiên SSO Keycloak. */
export async function logout(): Promise<void> {
  if (keycloak.authenticated) {
    try {
      await api.post('/auth/logout')
    } catch (failure) {
      if (
        !axios.isAxiosError(failure) ||
        ![401, 403].includes(failure.response?.status ?? 0)
      )
        throw failure
    }
  }
  await queryClient.cancelQueries()
  queryClient.clear()
  notifyAuthenticationChanged('logout')
  await keycloak.logout({ redirectUri: authenticationRedirect('/login') })
}

function hasUnsafeCharacter(value: string): boolean {
  return Array.from(value).some((character) => {
    const code = character.charCodeAt(0)
    return character === '\\' || code <= 32 || (code >= 127 && code <= 159)
  })
}

/** Chỉ nhận URL nội bộ; chặn URL giao thức, dấu gạch chéo ngược và ký tự điều khiển. */
export function safeInternalRedirect(value: string | null): string | null {
  if (!value || !value.startsWith('/') || value.startsWith('//')) return null
  let decoded: string
  try {
    decoded = decodeURIComponent(value)
  } catch {
    return null
  }
  if (hasUnsafeCharacter(value) || hasUnsafeCharacter(decoded)) return null
  if (decoded.startsWith('//')) return null
  const base = 'https://dms.invalid'
  const url = new URL(value, base)
  if (url.origin !== base) return null
  if (
    ['/login', '/forgot-password', CHANGE_PASSWORD_PATH].includes(url.pathname)
  )
    return null
  return url.pathname + url.search + url.hash
}

/** Bắt đổi mật khẩu trước; Admin có thể quay lại cổng quản trị, quản lý hoặc kỹ thuật. */
export function loginLandingPath(me: Me, requested: string | null): string {
  // Role lạ không được dùng để tạo URL động; guard sẽ giải thích lỗi sau khi tải /me.
  if (!Object.hasOwn(Role, me.role)) return '/login'
  if (me.mustChangePassword) return CHANGE_PASSWORD_PATH
  const target = safeInternalRedirect(requested)
  const portals =
    me.role === Role.ADMIN
      ? [
          rolePortal[Role.ADMIN],
          rolePortal[Role.STAFF],
          rolePortal[Role.TECHNICIAN],
        ]
      : [rolePortal[me.role]]
  const pathname = target?.split(/[?#]/)[0]
  return pathname &&
    portals.some(
      (portal) => pathname === portal || pathname.startsWith(`${portal}/`),
    ) &&
    hasAnyPermission(me, permissionsForPath(pathname))
    ? target!
    : roleHome[me.role]
}

export function loginPath(reason?: string, redirect?: string): string {
  const params = new URLSearchParams()
  if (reason) params.set('reason', reason)
  const safe = safeInternalRedirect(redirect ?? null)
  if (safe) params.set('redirect', safe)
  return `/login${params.size ? `?${params}` : ''}`
}

const eventKey = 'dms.auth-event'
type AuthEvent = 'logout' | 'password-changed' | 'account-changed'

/** Đồng bộ các tab cùng trình duyệt; chỉ lưu tín hiệu, không lưu mật khẩu hay token. */
export function notifyAuthenticationChanged(reason: AuthEvent) {
  try {
    localStorage.setItem(
      eventKey,
      JSON.stringify({ reason, nonce: crypto.randomUUID() }),
    )
  } catch {
    // Chặn storage không làm hỏng đăng nhập; máy chủ vẫn kiểm tra phiên ở mọi request.
  }
}

export function watchAuthenticationChanges() {
  const onStorage = (event: StorageEvent) => {
    if (event.key !== eventKey || !event.newValue) return
    try {
      const { reason } = JSON.parse(event.newValue) as { reason?: AuthEvent }
      if (
        !reason ||
        !['logout', 'password-changed', 'account-changed'].includes(reason)
      )
        return
      void queryClient.cancelQueries().then(() => {
        queryClient.clear()
        // Tải lại /me ở tab nhận sự kiện để nhận đúng tài khoản hoặc trạng thái chưa đăng nhập.
        window.location.replace(
          loginPath(reason === 'password-changed' ? reason : undefined),
        )
      })
    } catch {
      // Bỏ qua tín hiệu storage không đúng định dạng.
    }
  }
  window.addEventListener('storage', onStorage)
  return () => window.removeEventListener('storage', onStorage)
}
