import { useLocation } from 'react-router'
import { Role } from '../constants/authorization'
import { rolePortal } from '../constants/navigation'

/**
 * Cổng của màn đang hiển thị, suy từ tiền tố đường dẫn.
 * Admin dùng lại màn của Staff nên không được hardcode Role.STAFF,
 * nếu không mọi liên kết điều hướng trỏ về `/staff` và bị chặn.
 */
export function portalRoleOf(pathname: string): Role {
  const prefix = `/${pathname.split('/')[1] ?? ''}`
  return (
    Object.values(Role).find((role) => rolePortal[role] === prefix) ?? Role.STAFF
  )
}

export function usePortalRole(): Role {
  return portalRoleOf(useLocation().pathname)
}
