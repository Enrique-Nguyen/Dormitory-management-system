import { adminNav } from '../../constants/navigation'
import SidebarShell from './SidebarShell'

// Admin quản lý mọi nghiệp vụ, dùng chung các màn CRUD và thông báo.
export default function AdminLayout() {
  return (
    <SidebarShell
      groups={adminNav}
      portalName="Quản trị"
      homePath="/admin/users"
      notificationsPath="/admin/notifications"
    />
  )
}
