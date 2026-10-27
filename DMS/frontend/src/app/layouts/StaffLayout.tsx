import { useNavigate } from 'react-router'
import {
  STAFF_INBOX_PATH,
  STAFF_PENDING_ITEMS_PATH,
  staffNav,
} from '../../constants/navigation'
import { usePendingItemsBadge } from '../../features/contract'
import SidebarShell from './SidebarShell'

export default function StaffLayout() {
  const navigate = useNavigate()
  const badges = usePendingItemsBadge(STAFF_PENDING_ITEMS_PATH)
  return (
    <SidebarShell
      groups={staffNav}
      badges={badges}
      portalName="Quản lý"
      homePath="/staff/dashboard"
      notificationsPath={STAFF_INBOX_PATH}
      searchPlaceholder="Tìm sinh viên (mã SV, họ tên)..."
      onSearch={(q) => q.trim() && navigate(`/staff/students?q=${encodeURIComponent(q.trim())}`)}
    />
  )
}
