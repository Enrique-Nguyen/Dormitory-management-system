import type { RouteObject } from 'react-router'
import { Navigate } from 'react-router'
import PlaceholderPage from '../../pages/PlaceholderPage'
import AdminHomePage from '../../pages/admin/AdminHomePage'
import RequireRole from '../guards/RequireRole'
import AdminLayout from '../layouts/AdminLayout'

// UI mục 23, 26. Toàn bộ cổng Admin do TV1 phụ trách (UI 27.3).
export const adminRoutes: RouteObject = {
  path: '/admin',
  element: <RequireRole role="ADMIN" />,
  children: [
    {
      element: <AdminLayout />,
      children: [
        { index: true, element: <Navigate to="users" replace /> },
        { path: 'users', element: <AdminHomePage /> },
        { path: 'units', element: <PlaceholderPage title="Đơn vị quản lý" owner="TV1" spec="UI mục 23" /> },
        { path: 'buildings', element: <PlaceholderPage title="Tòa nhà & mức cọc" owner="TV1" spec="UI mục 23; BR-10" /> },
        { path: 'import', element: <PlaceholderPage title="Import sinh viên" owner="TV1" spec="UI mục 23; FR-ACC-05" /> },
        { path: 'aggregate-permissions', element: <PlaceholderPage title="Quyền tổng hợp liên đơn vị" owner="TV1" spec="FR-ACC-08" /> },
        { path: 'audit-logs', element: <PlaceholderPage title="Nhật ký hệ thống" owner="TV1" spec="UI mục 23" /> },
      ],
    },
  ],
}
