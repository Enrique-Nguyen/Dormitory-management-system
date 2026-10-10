import type { RouteObject } from 'react-router'
import PlaceholderPage from '../../pages/PlaceholderPage'
import StudentHomePage from '../../pages/student/StudentHomePage'
import RequireRole from '../guards/RequireRole'
import StudentLayout from '../layouts/StudentLayout'

// UI mục 26. Thay `element` bằng trang thật của bạn (pages/student/...). Phụ trách theo UI 27.3.
export const studentRoutes: RouteObject = {
  path: '/student',
  element: <RequireRole role="STUDENT" />,
  children: [
    {
      element: <StudentLayout />,
      children: [
        { index: true, element: <StudentHomePage /> },
        { path: 'room', element: <PlaceholderPage title="Phòng của tôi" owner="TV2" spec="UI mục 7" /> },
        { path: 'applications', element: <PlaceholderPage title="Đơn đăng ký" owner="TV3" spec="UI mục 8; FR-REG" /> },
        { path: 'applications/:id', element: <PlaceholderPage title="Chi tiết đơn đăng ký" owner="TV3" spec="UI mục 8 (timeline)" /> },
        { path: 'contracts', element: <PlaceholderPage title="Hợp đồng" owner="TV3" spec="UI mục 9; FR-CON" /> },
        { path: 'contracts/:id', element: <PlaceholderPage title="Chi tiết hợp đồng" owner="TV3" spec="UI mục 9, 16 (lịch sử phân phòng)" /> },
        { path: 'requests', element: <PlaceholderPage title="Yêu cầu gia hạn / chuyển phòng" owner="TV3" spec="UI mục 22A; FR-CON-06, 07" /> },
        { path: 'invoices', element: <PlaceholderPage title="Hóa đơn" owner="TV5" spec="UI mục 10; FR-BIL" /> },
        { path: 'deposit', element: <PlaceholderPage title="Tiền cọc" owner="TV5" spec="UI mục 9 (sổ cọc)" /> },
        { path: 'maintenance', element: <PlaceholderPage title="Báo hỏng" owner="TV4" spec="UI mục 11; FR-MNT" /> },
        { path: 'maintenance/:id', element: <PlaceholderPage title="Chi tiết ticket" owner="TV4" spec="UI mục 11" /> },
        { path: 'summer', element: <PlaceholderPage title="Lựa chọn kỳ hè" owner="TV3" spec="UI mục 22B" /> },
        { path: 'notifications', element: <PlaceholderPage title="Thông báo" owner="TV4" spec="UI mục 24; FR-NOT" /> },
        { path: 'profile', element: <PlaceholderPage title="Hồ sơ" owner="TV1" spec="UI mục 3.1; FR-ACC" /> },
      ],
    },
  ],
}
