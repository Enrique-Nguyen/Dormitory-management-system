import type { Role } from '../../constants/authorization'
import ContractDetailView from '../../features/contract/ContractDetailView'
import { usePortalRole } from '../../hooks/usePortalRole'

export interface ContractDetailPageProps {
  /** Cổng đang xem; mặc định suy từ tiền tố đường dẫn để Admin dùng lại màn Staff. */
  role?: Role
}

/** Trang chi tiết hợp đồng cho cổng quản lý. Route do main agent đăng ký. */
export default function ContractDetailPage({
  role,
}: ContractDetailPageProps = {}) {
  const portalRole = usePortalRole()
  return <ContractDetailView role={role ?? portalRole} />
}
