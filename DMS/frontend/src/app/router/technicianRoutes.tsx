import type { RouteObject } from 'react-router'
import { Navigate } from 'react-router'
import PlaceholderPage from '../../pages/PlaceholderPage'
import TechnicianHomePage from '../../pages/technician/TechnicianHomePage'
import RequireRole from '../guards/RequireRole'
import TechnicianLayout from '../layouts/TechnicianLayout'

// UI mục 22, 26. `notifications` không có trong danh sách route mục 26, nhưng UI 4.3 đặt chuông thông báo ở header KTV.
export const technicianRoutes: RouteObject = {
  path: '/technician',
  element: <RequireRole role="TECHNICIAN" />,
  children: [
    {
      element: <TechnicianLayout />,
      children: [
        { index: true, element: <Navigate to="tasks" replace /> },
        { path: 'tasks', element: <TechnicianHomePage /> },
        { path: 'tasks/:id', element: <PlaceholderPage title="Chi tiết công việc" owner="TV4" spec="UI mục 22" /> },
        { path: 'notifications', element: <PlaceholderPage title="Thông báo" owner="TV4" spec="UI mục 4.3, 24" /> },
      ],
    },
  ],
}
