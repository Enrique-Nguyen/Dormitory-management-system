import { Role } from '../../constants/authorization'
import type { RouteObject } from 'react-router'
import CrudPage from '../../pages/CrudPage'
import CrudTabsPage from '../../pages/CrudTabsPage'
import NotificationInboxPage from '../../pages/NotificationInboxPage'
import RoomDetailPage from '../../pages/staff/RoomDetailPage'
import StaffApplicationDetailPage from '../../pages/staff/ApplicationDetailPage'
import StaffContractDetailPage from '../../pages/staff/ContractDetailPage'
import StaffExtensionDetailPage from '../../pages/staff/ExtensionDetailPage'
import StaffHomePage from '../../pages/staff/StaffHomePage'
import StaffMaintenanceDetailPage from '../../pages/staff/MaintenanceDetailPage'
import MoveOutPage from '../../pages/staff/MoveOutPage'
import PendingItemsPage from '../../pages/staff/PendingItemsPage'
import StaffSummerChoiceDetailPage from '../../pages/staff/SummerChoiceDetailPage'
import StoragePage from '../../pages/staff/StoragePage'
import TransferReviewPage from '../../pages/staff/TransferReviewPage'
import RequireRole from '../guards/RequireRole'
import HomeRedirect from '../guards/HomeRedirect'
import StaffLayout from '../layouts/StaffLayout'

// UI mục 26. Thay `element` bằng trang thật của bạn (pages/staff/...). Phụ trách theo UI 27.3.
export const staffRoutes: RouteObject = {
  path: '/staff',
  element: <RequireRole role={Role.STAFF} />,
  children: [
    {
      element: <StaffLayout />,
      children: [
        { index: true, element: <HomeRedirect /> },
        { path: 'dashboard', element: <StaffHomePage /> },
        // Hồ sơ cá nhân dùng chung; backend chỉ đọc/sửa thông tin của người đang đăng nhập.
        { path: 'profile', element: <CrudPage resource="profile" /> },
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
        { path: 'applications', element: <CrudPage resource="applications" /> },
        {
          path: 'applications/:id',
          element: <StaffApplicationDetailPage />,
        },
        { path: 'rooms', element: <CrudPage resource="rooms" /> },
        { path: 'rooms/:id', element: <RoomDetailPage /> },
        { path: 'transfers/:id', element: <TransferReviewPage /> },
        { path: 'students', element: <CrudPage resource="students" /> },
        { path: 'contracts', element: <CrudPage resource="contracts" /> },
        {
          path: 'contracts/:id',
          element: <StaffContractDetailPage />,
        },
        {
          path: 'contracts/:id/move-out',
          element: <MoveOutPage />,
        },
        {
          path: 'extensions/:id',
          element: <StaffExtensionDetailPage />,
        },
        { path: 'summer', element: <CrudPage resource="summer" /> },
        {
          path: 'summer/:id',
          element: <StaffSummerChoiceDetailPage />,
        },
        { path: 'storage', element: <StoragePage /> },
        { path: 'utilities', element: <CrudPage resource="readings" /> },
        { path: 'utilities/prices', element: <CrudPage resource="rates" /> },
        { path: 'invoices', element: <CrudPage resource="invoices" /> },
        { path: 'invoices/issue', element: <CrudPage resource="invoices" /> },
        { path: 'deposits', element: <CrudPage resource="deposits" /> },
        { path: 'maintenance', element: <CrudPage resource="maintenance" /> },
        {
          path: 'maintenance/:id',
          element: <StaffMaintenanceDetailPage />,
        },
        {
          path: 'notifications',
          element: <CrudPage resource="notifications" />,
        },
        { path: 'inbox', element: <NotificationInboxPage /> },
        { path: 'pending-items', element: <PendingItemsPage /> },
        { path: 'room-types', element: <CrudPage resource="roomTypes" /> },
        { path: 'semesters', element: <CrudPage resource="semesters" /> },
        {
          path: 'registration-periods',
          element: <CrudPage resource="periods" />,
        },
      ],
    },
  ],
}
