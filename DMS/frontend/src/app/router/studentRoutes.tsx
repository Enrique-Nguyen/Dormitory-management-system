import { Role } from '../../constants/authorization'
import type { RouteObject } from 'react-router'
import RequireRole from '../guards/RequireRole'
import StudentLayout from '../layouts/StudentLayout'
import { lazyPage } from './lazyPage'

const CrudPage = lazyPage(() => import('../../pages/CrudPage'))
const CrudTabsPage = lazyPage(() => import('../../pages/CrudTabsPage'))
const NotificationInboxPage = lazyPage(() => import('../../pages/NotificationInboxPage'))
const StudentApplicationDetailPage = lazyPage(() => import('../../pages/student/ApplicationDetailPage'))
const StudentContractDetailPage = lazyPage(() => import('../../pages/student/ContractDetailPage'))
const StudentExtensionDetailPage = lazyPage(() => import('../../pages/student/ExtensionDetailPage'))
const StudentHomePage = lazyPage(() => import('../../pages/student/StudentHomePage'))
const StudentMaintenanceDetailPage = lazyPage(() => import('../../pages/student/MaintenanceDetailPage'))
const StudentSummerChoiceDetailPage = lazyPage(() => import('../../pages/student/SummerChoiceDetailPage'))
const TransferRequestPage = lazyPage(() => import('../../pages/student/TransferRequestPage'))

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
