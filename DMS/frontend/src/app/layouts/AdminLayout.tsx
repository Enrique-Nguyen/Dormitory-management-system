import { Role } from '../../constants/authorization'
import {
  adminNav,
  portalPath,
  STAFF_PENDING_ITEMS_PATH,
} from '../../constants/navigation'
import { usePendingItemsBadge } from '../../features/contract'
import SidebarShell from './SidebarShell'

// Admin quản lý mọi nghiệp vụ, dùng chung các màn CRUD và thông báo.
export default function AdminLayout() {
  const badges = usePendingItemsBadge(
    portalPath(Role.ADMIN, STAFF_PENDING_ITEMS_PATH),
  )
  return (
    <SidebarShell
      groups={adminNav}
      badges={badges}
      portalName="Quản trị"
      homePath="/admin/users"
      notificationsPath="/admin/inbox"
    />
  )
}
