import { Role } from '../../constants/authorization'
import type { RouteObject } from 'react-router'
import CrudPage from '../../pages/CrudPage'
import CrudTabsPage from '../../pages/CrudTabsPage'
import RoomDetailPage from '../../pages/staff/RoomDetailPage'
import StaffHomePage from '../../pages/staff/StaffHomePage'
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
          element: <CrudPage resource="applications" />,
        },
        { path: 'rooms', element: <CrudPage resource="rooms" /> },
        { path: 'rooms/:id', element: <RoomDetailPage /> },
        { path: 'transfers/:id', element: <TransferReviewPage /> },
        { path: 'students', element: <CrudPage resource="students" /> },
        { path: 'contracts', element: <CrudPage resource="contracts" /> },
        { path: 'contracts/:id', element: <CrudPage resource="contracts" /> },
        { path: 'summer', element: <CrudPage resource="summer" /> },
        { path: 'storage', element: <CrudPage resource="storage" /> },
        { path: 'utilities', element: <CrudPage resource="readings" /> },
        { path: 'utilities/prices', element: <CrudPage resource="rates" /> },
        { path: 'invoices', element: <CrudPage resource="invoices" /> },
        { path: 'invoices/issue', element: <CrudPage resource="invoices" /> },
        { path: 'deposits', element: <CrudPage resource="deposits" /> },
        { path: 'maintenance', element: <CrudPage resource="maintenance" /> },
        {
          path: 'notifications',
          element: <CrudPage resource="notifications" />,
        },
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
