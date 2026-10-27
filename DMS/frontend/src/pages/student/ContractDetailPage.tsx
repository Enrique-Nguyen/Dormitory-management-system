import ContractDetailView from '../../features/contract/ContractDetailView'

/**
 * Trang chi tiết hợp đồng của sinh viên. View tự lấy vai trò từ phiên đăng nhập nên không cần
 * truyền role.
 */
export default function StudentContractDetailPage() {
  return <ContractDetailView />
}
