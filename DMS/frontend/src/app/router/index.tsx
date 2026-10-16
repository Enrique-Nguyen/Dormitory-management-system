import { createBrowserRouter } from 'react-router'
import ChangePasswordPage from '../../pages/ChangePasswordPage'
import LoginPage, { ForgotPasswordPage } from '../../pages/LoginPage'
import HomeRedirect from '../guards/HomeRedirect'
import RequireRole from '../guards/RequireRole'
import { adminRoutes } from './adminRoutes'
import { staffRoutes } from './staffRoutes'
import { studentRoutes } from './studentRoutes'
import { technicianRoutes } from './technicianRoutes'

// Bốn cổng theo vai trò (SAD mục 5, UI mục 26). Mỗi vai trò có file route riêng để các thành viên
// sửa song song, ít xung đột merge: thay `element` của route bằng trang thật khi làm xong màn.
export const router = createBrowserRouter([
  { path: '/login', element: <LoginPage /> },
  { path: '/forgot-password', element: <ForgotPasswordPage /> },
  {
    // Đổi mật khẩu: mọi vai trò, kể cả tài khoản đang bị bắt buộc đổi (FR-ACC-03)
    element: <RequireRole allowMustChangePassword />,
    children: [{ path: '/change-password', element: <ChangePasswordPage /> }],
  },
  studentRoutes,
  staffRoutes,
  technicianRoutes,
  adminRoutes,
  {
    // `/` và đường dẫn không tồn tại: đã đăng nhập → trang chủ vai trò; chưa → /login (qua guard)
    element: <RequireRole />,
    children: [{ path: '*', element: <HomeRedirect /> }],
  },
])
