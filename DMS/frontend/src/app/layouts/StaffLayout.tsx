import { useNavigate } from 'react-router'
import { staffNav } from '../../constants/navigation'
import SidebarShell from './SidebarShell'

export default function StaffLayout() {
  const navigate = useNavigate()
  // TODO(TV3): badges={{ [STAFF_REQUESTS_PATH]: pendingCount }} khi có API hộp "Yêu cầu chờ xử lý"
  return (
    <SidebarShell
      groups={staffNav}
      portalName="Quản lý"
      homePath="/staff/dashboard"
      notificationsPath="/staff/notifications"
      searchPlaceholder="Tìm sinh viên (mã SV, họ tên)..."
      onSearch={(q) => q.trim() && navigate(`/staff/students?q=${encodeURIComponent(q.trim())}`)}
    />
  )
}
