// Feature contract (TV3): hook TanStack Query cho chi tiết/kích hoạt hợp đồng, duyệt gia hạn và
// hộp yêu cầu chờ xử lý. Hàm gọi API nằm ở services/api/contract.ts.
import {
  keepPreviousData,
  useMutation,
  useQuery,
  useQueryClient,
  type QueryClient,
} from '@tanstack/react-query'
import { useApiErrorHandler } from '../../hooks/useApiErrorHandler'
import { Permission } from '../../constants/authorization'
import { useMe } from '../account/useMe'
import { hasAnyPermission } from '../../services/api/auth'
import {
  activateContract,
  approveExtension,
  cancelExtension,
  createExtensionRequest,
  getContract,
  getExtensionRequest,
  rejectExtension,
  searchPendingItems,
  type ActivateContractRequest,
  type ExtensionDetail,
  type PendingItemsRequest,
  type SaveExtensionRequest,
} from '../../services/api/contract'

export const contractKeys = {
  detail: (id: number) => ['contract', id] as const,
}

export const extensionKeys = {
  detail: (id: number) => ['extension-request', id] as const,
}

export const pendingItemKeys = {
  list: (request: PendingItemsRequest) => ['pending-items', request] as const,
}

/**
 * Sau kích hoạt/duyệt gia hạn, hợp đồng, danh sách CRUD, Dashboard, sổ cọc và hộp việc đều có thể
 * đổi. Làm mới nền các nhóm dữ liệu liên quan, không giữ hộp thoại của người dùng.
 */
export function invalidateContractQueries(queryClient: QueryClient) {
  for (const queryKey of [
    ['contracts'],
    ['contract'],
    ['extension-requests'],
    ['extension-request'],
    ['summer-choices'],
    ['summer-choice'],
    ['pending-items'],
    ['crud'],
    ['staff'],
    ['deposits'],
    ['notification'],
    ['notifications'],
  ]) {
    void queryClient.invalidateQueries({ queryKey })
  }
}

export function useContractDetail(id: number, enabled = true) {
  return useQuery({
    queryKey: contractKeys.detail(id),
    queryFn: () => getContract(id),
    enabled: enabled && Number.isSafeInteger(id) && id > 0,
  })
}

export function useExtensionRequest(id: number, enabled = true) {
  return useQuery({
    queryKey: extensionKeys.detail(id),
    queryFn: () => getExtensionRequest(id),
    enabled: enabled && Number.isSafeInteger(id) && id > 0,
  })
}

export function useActivateContract(id: number) {
  const queryClient = useQueryClient()
  const onError = useApiErrorHandler()
  return useMutation({
    mutationFn: (input: ActivateContractRequest) => activateContract(id, input),
    onSuccess: (detail) => {
      invalidateContractQueries(queryClient)
      return detail
    },
    onError,
  })
}

/** Sinh viên gửi yêu cầu gia hạn; hợp đồng nguồn lấy từ chi tiết đang mở. */
export function useCreateExtensionRequest(contractId: number) {
  const queryClient = useQueryClient()
  const onError = useApiErrorHandler()
  return useMutation({
    mutationFn: (input: Omit<SaveExtensionRequest, 'contractId'>) =>
      createExtensionRequest({ ...input, contractId }),
    onSuccess: () => {
      invalidateContractQueries(queryClient)
      void queryClient.invalidateQueries({
        queryKey: contractKeys.detail(contractId),
      })
    },
    onError,
  })
}

function useExtensionDecision(
  id: number,
  mutationFn: () => Promise<ExtensionDetail>,
) {
  const queryClient = useQueryClient()
  const onError = useApiErrorHandler()
  return useMutation({
    mutationFn,
    onSuccess: (detail) => {
      invalidateContractQueries(queryClient)
      void queryClient.invalidateQueries({ queryKey: extensionKeys.detail(id) })
      return detail
    },
    onError,
  })
}

export function useApproveExtension(id: number) {
  return useExtensionDecision(id, () => approveExtension(id))
}

export function useRejectExtension(id: number) {
  const queryClient = useQueryClient()
  const onError = useApiErrorHandler()
  return useMutation({
    mutationFn: (reason: string) => rejectExtension(id, reason),
    onSuccess: (detail) => {
      invalidateContractQueries(queryClient)
      void queryClient.invalidateQueries({ queryKey: extensionKeys.detail(id) })
      return detail
    },
    onError,
  })
}

export function useCancelExtension(id: number) {
  return useExtensionDecision(id, () => cancelExtension(id))
}

export function usePendingItems(
  request: PendingItemsRequest,
  enabled = true,
) {
  return useQuery({
    queryKey: pendingItemKeys.list(request),
    queryFn: () => searchPendingItems(request),
    enabled,
    placeholderData: keepPreviousData,
  })
}

/** Tổng số việc còn chờ, chỉ lấy một dòng để gắn badge trên menu. */
export function usePendingItemCount(enabled = true) {
  const items = usePendingItems({ page: 0, size: 1 }, enabled)
  return items.data?.totalElements ?? 0
}

/**
 * Badge số việc chờ cho mục menu `path` (UI 22A). Backend mở hộp việc cho quyền đọc hợp đồng hoặc
 * đọc đơn đăng ký, nên ngoài hai quyền đó menu không hỏi API.
 */
export function usePendingItemsBadge(path: string): Record<string, number> {
  const { data: me } = useMe()
  const allowed = hasAnyPermission(me, [
    Permission.CONTRACT_READ,
    Permission.ROOM_APPLICATION_READ,
  ])
  return { [path]: usePendingItemCount(allowed) }
}
