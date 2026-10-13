import type { RouteObject } from 'react-router'
import { Navigate } from 'react-router'
import CrudPage from '../../pages/CrudPage'
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
        { path: 'users', element: <CrudPage resource="users" /> },
        { path: 'units', element: <CrudPage resource="units" /> },
        { path: 'buildings', element: <CrudPage resource="buildings" /> },
        { path: 'import', element: <CrudPage resource="adminStudents" /> },
        {
          path: 'aggregate-permissions',
          element: <CrudPage resource="users" />,
        },
        {
          path: 'priority-categories',
          element: <CrudPage resource="priorities" />,
        },
        { path: 'configs', element: <CrudPage resource="configs" /> },
        { path: 'audit-logs', element: <CrudPage resource="audit" /> },
      ],
    },
  ],
}
