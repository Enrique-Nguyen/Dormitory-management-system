import { adminNav } from '../../constants/navigation'
import SidebarShell from './SidebarShell'

// Admin không có thông báo nghiệp vụ và không có tìm kiếm chung (UI mục 23).
export default function AdminLayout() {
  return <SidebarShell groups={adminNav} portalName="Quản trị" homePath="/admin/users" />
}
