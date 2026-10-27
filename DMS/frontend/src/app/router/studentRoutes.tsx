import { Role } from '../../constants/authorization'
import type { RouteObject } from 'react-router'
import CrudPage from '../../pages/CrudPage'
import CrudTabsPage from '../../pages/CrudTabsPage'
import NotificationInboxPage from '../../pages/NotificationInboxPage'
import StudentApplicationDetailPage from '../../pages/student/ApplicationDetailPage'
import StudentContractDetailPage from '../../pages/student/ContractDetailPage'
import StudentExtensionDetailPage from '../../pages/student/ExtensionDetailPage'
import StudentHomePage from '../../pages/student/StudentHomePage'
import StudentMaintenanceDetailPage from '../../pages/student/MaintenanceDetailPage'
import StudentSummerChoiceDetailPage from '../../pages/student/SummerChoiceDetailPage'
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
          element: <StudentApplicationDetailPage />,
        },
        { path: 'contracts', element: <CrudPage resource="myContracts" /> },
        {
          path: 'contracts/:id',
          element: <StudentContractDetailPage />,
        },
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
        {
          path: 'requests/extensions/:id',
          element: <StudentExtensionDetailPage />,
        },
        { path: 'invoices', element: <CrudPage resource="myInvoices" /> },
        { path: 'deposit', element: <CrudPage resource="myDeposits" /> },
        { path: 'maintenance', element: <CrudPage resource="maintenance" /> },
        {
          path: 'maintenance/:id',
          element: <StudentMaintenanceDetailPage />,
        },
        { path: 'summer', element: <CrudPage resource="summer" /> },
        {
          path: 'summer/:id',
          element: <StudentSummerChoiceDetailPage />,
        },
        { path: 'notifications', element: <NotificationInboxPage /> },
        { path: 'profile', element: <CrudPage resource="profile" /> },
      ],
    },
  ],
}
