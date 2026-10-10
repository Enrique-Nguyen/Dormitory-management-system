import { Button, Result, Spin } from 'antd'
import axios from 'axios'
import { Navigate, Outlet, useLocation } from 'react-router'
import { useMe } from '../../features/account/useMe'
import { CHANGE_PASSWORD_PATH, roleHome } from '../../constants/navigation'
import type { Role } from '../../types/auth'

interface Props {
  /** Vai trò được vào; bỏ trống = mọi người đã đăng nhập (dùng cho /change-password). */
  role?: Role
  /** Cho phép tài khoản đang bị bắt buộc đổi mật khẩu (chỉ trang đổi mật khẩu). */
  allowMustChangePassword?: boolean
}

/**
 * Guard của mỗi cổng (UI 27.1). Thứ tự: chưa đăng nhập → /login; bắt buộc đổi mật khẩu → /change-password
 * (FR-ACC-03); sai vai trò → trang chủ của vai trò đó (không hiện 403).
 */
export default function RequireRole({ role, allowMustChangePassword = false }: Props) {
  const { data: me, isPending, error, refetch } = useMe()
  const location = useLocation()

  if (isPending) {
    return (
      <div style={{ display: 'grid', placeItems: 'center', minHeight: '100vh' }}>
        <Spin size="large" aria-label="Đang tải" />
      </div>
    )
  }

  if (error || !me) {
    if (axios.isAxiosError(error) && error.response?.status === 401) {
      const back = encodeURIComponent(location.pathname + location.search)
      return <Navigate to={`/login?redirect=${back}`} replace />
    }
    return (
      <Result
        status="error"
        title="Không kết nối được tới máy chủ"
        subTitle="Kiểm tra kết nối mạng rồi thử lại."
        extra={<Button type="primary" onClick={() => refetch()}>Thử lại</Button>}
      />
    )
  }

  if (me.mustChangePassword && !allowMustChangePassword) return <Navigate to={CHANGE_PASSWORD_PATH} replace />
  if (role && me.role !== role) return <Navigate to={roleHome[me.role]} replace />

  return <Outlet />
}
