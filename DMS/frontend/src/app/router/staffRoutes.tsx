import type { RouteObject } from 'react-router'
import { Navigate } from 'react-router'
import PlaceholderPage from '../../pages/PlaceholderPage'
import StaffHomePage from '../../pages/staff/StaffHomePage'
import RequireRole from '../guards/RequireRole'
import StaffLayout from '../layouts/StaffLayout'

// UI mục 26. Thay `element` bằng trang thật của bạn (pages/staff/...). Phụ trách theo UI 27.3.
export const staffRoutes: RouteObject = {
  path: '/staff',
  element: <RequireRole role="STAFF" />,
  children: [
    {
      element: <StaffLayout />,
      children: [
        { index: true, element: <Navigate to="dashboard" replace /> },
        { path: 'dashboard', element: <StaffHomePage /> },
        { path: 'requests', element: <PlaceholderPage title="Yêu cầu chờ xử lý" owner="TV3" spec="UI mục 22A" /> },
        { path: 'applications', element: <PlaceholderPage title="Đơn đăng ký" owner="TV3" spec="UI mục 13" /> },
        { path: 'applications/:id', element: <PlaceholderPage title="Chi tiết đơn đăng ký" owner="TV3 + TV2" spec="UI mục 14 (xét duyệt, chọn giường)" /> },
        { path: 'rooms', element: <PlaceholderPage title="Phòng & Giường" owner="TV2" spec="UI mục 15" /> },
        { path: 'rooms/:id', element: <PlaceholderPage title="Chi tiết phòng" owner="TV2" spec="UI mục 15 (trưởng phòng, lịch sử)" /> },
        { path: 'students', element: <PlaceholderPage title="Sinh viên" owner="Chưa phân công" spec="UI mục 3.2; FR-ACC" /> },
        { path: 'contracts', element: <PlaceholderPage title="Hợp đồng" owner="TV3" spec="UI mục 16" /> },
        { path: 'contracts/:id', element: <PlaceholderPage title="Chi tiết hợp đồng" owner="TV3 + TV4" spec="UI mục 16, 16.1 (kích hoạt), 17 (trả phòng)" /> },
        { path: 'summer', element: <PlaceholderPage title="Kỳ hè" owner="TV3" spec="UI mục 22B" /> },
        { path: 'storage', element: <PlaceholderPage title="Kho gửi đồ" owner="TV4" spec="UI mục 22B (phiếu gửi đồ)" /> },
        { path: 'utilities', element: <PlaceholderPage title="Điện nước" owner="TV5" spec="UI mục 18" /> },
        { path: 'utilities/prices', element: <PlaceholderPage title="Đơn giá điện nước" owner="TV5" spec="UI mục 19.3; FR-BIL-01" /> },
        { path: 'invoices', element: <PlaceholderPage title="Hóa đơn" owner="TV5" spec="UI mục 19" /> },
        { path: 'invoices/issue', element: <PlaceholderPage title="Phát hành hóa đơn" owner="TV5" spec="UI mục 19.1; UC-21" /> },
        { path: 'deposits', element: <PlaceholderPage title="Tiền cọc / Công nợ" owner="TV5" spec="UI mục 20" /> },
        { path: 'maintenance', element: <PlaceholderPage title="Sửa chữa" owner="TV4" spec="UI mục 21" /> },
        { path: 'notifications', element: <PlaceholderPage title="Thông báo" owner="TV4" spec="UI mục 24; FR-NOT" /> },
        { path: 'room-types', element: <PlaceholderPage title="Loại phòng" owner="TV2" spec="FR-ROOM-02" /> },
        { path: 'semesters', element: <PlaceholderPage title="Kỳ học" owner="TV2" spec="FR-ROOM-11" /> },
        { path: 'registration-periods', element: <PlaceholderPage title="Đợt đăng ký" owner="TV3" spec="API Spec mục 4; FR-REG" /> },
      ],
    },
  ],
}
