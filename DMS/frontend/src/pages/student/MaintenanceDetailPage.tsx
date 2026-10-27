import { Role } from '../../constants/authorization'
import MaintenanceDetailView from '../../features/maintenance/MaintenanceDetailView'

/** Trang chi tiết ticket sửa chữa cho cổng sinh viên. */
export function StudentMaintenanceDetailPage() {
  return <MaintenanceDetailView role={Role.STUDENT} />
}

export default StudentMaintenanceDetailPage
