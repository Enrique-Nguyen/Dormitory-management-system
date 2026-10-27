import MaintenanceDetailView from '../../features/maintenance/MaintenanceDetailView'
import { usePortalRole } from '../../hooks/usePortalRole'

/** Trang chi tiết ticket sửa chữa cho cổng quản lý. Route do main agent đăng ký. */
export function StaffMaintenanceDetailPage() {
  return <MaintenanceDetailView role={usePortalRole()} />
}

export default StaffMaintenanceDetailPage
