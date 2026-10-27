import type { Role } from '../../constants/authorization'
import SummerChoiceDetailView from '../../features/registration/SummerChoiceDetailView'
import { usePortalRole } from '../../hooks/usePortalRole'

export interface SummerChoiceDetailPageProps {
  /** Cổng đang xem; mặc định suy từ tiền tố đường dẫn để Admin dùng lại màn Staff. */
  role?: Role
}

/** Trang chi tiết lựa chọn kỳ hè cho cổng quản lý. Route do main agent đăng ký. */
export default function SummerChoiceDetailPage({
  role,
}: SummerChoiceDetailPageProps = {}) {
  const portalRole = usePortalRole()
  return <SummerChoiceDetailView role={role ?? portalRole} />
}
