import { Role } from '../../constants/authorization'
import ApplicationDetailView from '../../features/registration/ApplicationDetailView'

/** Trang chi tiết đơn đăng ký cho cổng sinh viên. */
export function StudentApplicationDetailPage() {
  return <ApplicationDetailView role={Role.STUDENT} />
}

export default StudentApplicationDetailPage
