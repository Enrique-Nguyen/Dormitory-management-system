import { Role } from '../../constants/authorization'
import StaffExtensionDetailPage from '../staff/ExtensionDetailPage'

/**
 * Màn chi tiết yêu cầu gia hạn của sinh viên: chỉ xem và hủy, không có nút xét duyệt.
 */
export default function StudentExtensionDetailPage() {
  return <StaffExtensionDetailPage role={Role.STUDENT} />
}
