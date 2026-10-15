import { Role } from '../../constants/authorization'
import type { RouteObject } from 'react-router'
import { Navigate } from 'react-router'
import CrudPage from '../../pages/CrudPage'
import RolePermissionsPage from '../../pages/admin/RolePermissionsPage'
import RequireRole from '../guards/RequireRole'
import AdminLayout from '../layouts/AdminLayout'
import { staffRoutes } from './staffRoutes'

// UI mục 23, 26. Toàn bộ cổng Admin do TV1 phụ trách (UI 27.3).
export const adminRoutes: RouteObject = {
  path: '/admin',
  element: <RequireRole role={Role.ADMIN} />,
  children: [
    {
      element: <AdminLayout />,
      children: [
        { index: true, element: <Navigate to="users" replace /> },
        { path: 'users', element: <CrudPage resource="users" /> },
        { path: 'units', element: <CrudPage resource="units" /> },
        { path: 'buildings', element: <CrudPage resource="buildings" /> },
        { path: 'import', element: <CrudPage resource="adminStudents" /> },
        {
          path: 'aggregate-permissions',
          element: <CrudPage resource="users" />,
        },
        { path: 'role-permissions', element: <RolePermissionsPage /> },
        {
          path: 'priority-categories',
          element: <CrudPage resource="priorities" />,
        },
        { path: 'configs', element: <CrudPage resource="configs" /> },
        { path: 'audit-logs', element: <CrudPage resource="audit" /> },
        // Admin dùng lại các màn nghiệp vụ; backend vẫn kiểm tra trạng thái và lịch sử.
        ...(staffRoutes.children?.[0].children ?? []).filter(
          (route) => !route.index && route.path !== 'students',
        ),
      ],
    },
  ],
}
