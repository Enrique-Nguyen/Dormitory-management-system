import { Role } from '../../constants/authorization'
import { Button, Result, Spin } from 'antd'
import axios from 'axios'
import { Navigate, Outlet, useLocation } from 'react-router'
import { useMe } from '../../features/account/useMe'
import {
  CHANGE_PASSWORD_PATH,
  permissionsForPath,
} from '../../constants/navigation'
import {
  accessibleHomePath,
  hasAnyPermission,
  loginPath,
} from '../../services/api/auth'
import { errorCodeOf } from '../../utils/errorMessages'

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
export default function RequireRole({
  role,
  allowMustChangePassword = false,
}: Props) {
  const { data: me, isPending, error, refetch } = useMe()
  const location = useLocation()

  if (isPending) {
    return (
      <div
        style={{ display: 'grid', placeItems: 'center', minHeight: '100vh' }}
      >
        <Spin size="large" aria-label="Đang tải" />
      </div>
    )
  }

  if (error || !me) {
    const code = errorCodeOf(error)
    // Lỗi kết nối cho thử lại; chỉ thiếu phiên/khóa thực sự mới đưa về đăng nhập.
    if (
      !error ||
      code === 'SESSION_REVOKED' ||
      code === 'ACCOUNT_LOCKED' ||
      (axios.isAxiosError(error) && error.response?.status === 401)
    ) {
      const reason =
        code === 'ACCOUNT_LOCKED'
          ? 'locked'
          : code === 'SESSION_REVOKED'
            ? 'revoked'
            : me
              ? 'expired'
              : undefined
      return (
        <Navigate
          to={loginPath(reason, location.pathname + location.search)}
          replace
        />
      )
    }
    return (
      <Result
        status="error"
        title="Không kết nối được tới máy chủ"
        subTitle="Kiểm tra kết nối mạng hoặc máy chủ rồi thử lại."
        extra={
          <Button type="primary" onClick={() => refetch()}>
            Thử lại
          </Button>
        }
      />
    )
  }

  // /me là dữ liệu từ API: từ chối role lạ trước khi dùng làm khóa chuyển hướng hoặc mở cổng.
  if (!Object.hasOwn(Role, me.role))
    return (
      <Result
        status="403"
        title="Vai trò tài khoản chưa được hỗ trợ"
        subTitle="Liên hệ quản trị viên để kiểm tra quyền của tài khoản."
      />
    )

  if (me.mustChangePassword && !allowMustChangePassword)
    return <Navigate to={CHANGE_PASSWORD_PATH} replace />
  if (
    role &&
    me.role !== role &&
    !(
      me.role === Role.ADMIN &&
      (role === Role.STAFF || role === Role.TECHNICIAN)
    )
  )
    return <Navigate to={accessibleHomePath(me)} replace />

  if (role && !hasAnyPermission(me, permissionsForPath(location.pathname)))
    return (
      <Result
        status="403"
        title="Không có quyền truy cập"
        subTitle="Tài khoản chưa được cấp quyền xem màn hình này."
        extra={
          <Button
            onClick={() => window.location.assign(accessibleHomePath(me))}
          >
            Về trang chủ
          </Button>
        }
      />
    )

  return <Outlet />
}
