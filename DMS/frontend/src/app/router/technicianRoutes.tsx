import { Role } from '../../constants/authorization'
import type { RouteObject } from 'react-router'
import RequireRole from '../guards/RequireRole'
import HomeRedirect from '../guards/HomeRedirect'
import TechnicianLayout from '../layouts/TechnicianLayout'
import { lazyPage } from './lazyPage'

const CrudPage = lazyPage(() => import('../../pages/CrudPage'))
const NotificationInboxPage = lazyPage(() => import('../../pages/NotificationInboxPage'))
const TaskDetailPage = lazyPage(() => import('../../pages/technician/TaskDetailPage'))

// UI mục 22, 26. `notifications` không có trong danh sách route mục 26, nhưng UI 4.3 đặt chuông thông báo ở header KTV.
export const technicianRoutes: RouteObject = {
  path: '/technician',
  element: <RequireRole role={Role.TECHNICIAN} />,
  children: [
    {
      element: <TechnicianLayout />,
      children: [
        { index: true, element: <HomeRedirect /> },
        { path: 'tasks', element: <CrudPage resource="maintenance" /> },
        { path: 'tasks/:id', element: <TaskDetailPage /> },
        { path: 'profile', element: <CrudPage resource="profile" /> },
        { path: 'notifications', element: <NotificationInboxPage /> },
      ],
    },
  ],
}
