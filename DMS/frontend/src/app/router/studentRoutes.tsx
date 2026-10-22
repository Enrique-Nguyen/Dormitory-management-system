import { Role } from '../../constants/authorization'
import type { RouteObject } from 'react-router'
import CrudPage from '../../pages/CrudPage'
import CrudTabsPage from '../../pages/CrudTabsPage'
import StudentHomePage from '../../pages/student/StudentHomePage'
import TransferRequestPage from '../../pages/student/TransferRequestPage'
import RequireRole from '../guards/RequireRole'
import StudentLayout from '../layouts/StudentLayout'

// UI mục 26. Thay `element` bằng trang thật của bạn (pages/student/...). Phụ trách theo UI 27.3.
export const studentRoutes: RouteObject = {
  path: '/student',
  element: <RequireRole role={Role.STUDENT} />,
  children: [
    {
      element: <StudentLayout />,
      children: [
        { index: true, element: <StudentHomePage /> },
        { path: 'room', element: <CrudPage resource="myRoom" /> },
        {
          path: 'applications',
          element: <CrudPage resource="myApplications" />,
        },
        {
          path: 'applications/:id',
          element: <CrudPage resource="myApplications" />,
        },
        { path: 'contracts', element: <CrudPage resource="myContracts" /> },
        { path: 'contracts/:id', element: <CrudPage resource="myContracts" /> },
        {
          path: 'requests',
          element: (
            <CrudTabsPage
              tabs={[
                { key: 'extensions', label: 'Gia hạn', resource: 'extensions' },
                {
                  key: 'transfers',
                  label: 'Chuyển phòng',
                  resource: 'transfers',
                },
              ]}
            />
          ),
        },
        { path: 'requests/transfers/:id', element: <TransferRequestPage /> },
        { path: 'invoices', element: <CrudPage resource="myInvoices" /> },
        { path: 'deposit', element: <CrudPage resource="myDeposits" /> },
        { path: 'maintenance', element: <CrudPage resource="maintenance" /> },
        {
          path: 'maintenance/:id',
          element: <CrudPage resource="maintenance" />,
        },
        { path: 'summer', element: <CrudPage resource="summer" /> },
        { path: 'notifications', element: <CrudPage resource="inbox" /> },
        { path: 'profile', element: <CrudPage resource="profile" /> },
      ],
    },
  ],
}
