import ApplicationDetailView from '../../features/registration/ApplicationDetailView'
import { usePortalRole } from '../../hooks/usePortalRole'

/** Trang chi tiết đơn đăng ký cho cổng quản lý. Route do main agent đăng ký. */
export function StaffApplicationDetailPage() {
  return <ApplicationDetailView role={usePortalRole()} />
}

export default StaffApplicationDetailPage
