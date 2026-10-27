import { Role } from '../../constants/authorization'
import MaintenanceDetailView from '../../features/maintenance/MaintenanceDetailView'

/** Trang chi tiết việc được giao cho cổng kỹ thuật viên. */
export function TaskDetailPage() {
  return <MaintenanceDetailView role={Role.TECHNICIAN} />
}

export default TaskDetailPage
