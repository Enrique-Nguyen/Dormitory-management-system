import { Role } from '../../constants/authorization'
import type { RouteObject } from 'react-router'
import { Navigate } from 'react-router'
import CrudPage from '../../pages/CrudPage'
import RequireRole from '../guards/RequireRole'
import TechnicianLayout from '../layouts/TechnicianLayout'

// UI mục 22, 26. `notifications` không có trong danh sách route mục 26, nhưng UI 4.3 đặt chuông thông báo ở header KTV.
export const technicianRoutes: RouteObject = {
  path: '/technician',
  element: <RequireRole role={Role.TECHNICIAN} />,
  children: [
    {
      element: <TechnicianLayout />,
      children: [
        { index: true, element: <Navigate to="tasks" replace /> },
        { path: 'tasks', element: <CrudPage resource="maintenance" /> },
        { path: 'tasks/:id', element: <CrudPage resource="maintenance" /> },
        { path: 'profile', element: <CrudPage resource="profile" /> },
        { path: 'notifications', element: <CrudPage resource="inbox" /> },
      ],
    },
  ],
}
