// Feature contract (TV3, TV2, TV4): hook TanStack Query cho nghiệp vụ hợp đồng.
// Hàm gọi API của module đặt ở services/api/contract.ts.
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  approveTransfer,
  cancelTransfer,
  getTransferRequest,
  rejectTransfer,
} from '../../services/api/contract'
import { useApiErrorHandler } from '../../hooks/useApiErrorHandler'

export { default as MoveOutModal, type MoveOutModalProps } from './MoveOutModal'
export {
  default as StorageModal,
  type StorageModalMode,
  type StorageModalProps,
} from './StorageModal'
export {
  default as ActivateContractModal,
  type ActivateContractModalProps,
} from './ActivateContractModal'
export {
  default as ContractDetailView,
  type ContractDetailViewProps,
} from './ContractDetailView'
export {
  default as ExtensionReviewModal,
  type ExtensionReviewModalProps,
} from './ExtensionReviewModal'
export {
  contractKeys,
  extensionKeys,
  invalidateContractQueries,
  pendingItemKeys,
  useActivateContract,
  useApproveExtension,
  useCancelExtension,
  useContractDetail,
  useCreateExtensionRequest,
  useExtensionRequest,
  usePendingItemCount,
  usePendingItems,
  usePendingItemsBadge,
  useRejectExtension,
} from './useContract'
export {
  checkoutKeys,
  useCreateStorageRecord,
  useExecuteMoveOut,
  useMoveOutPreview,
  useReturnStorageRecord,
  useStorageAvailableBeds,
  useStorageRecords,
  type StorageBedFilter,
} from './useCheckout'

export const transferKeys = {
  detail: (id: number) => ['transfer-request', id] as const,
}

export function useTransferRequest(id: number) {
  return useQuery({
    queryKey: transferKeys.detail(id),
    queryFn: () => getTransferRequest(id),
    enabled: Number.isSafeInteger(id) && id > 0,
  })
}

/**
 * Sau mỗi quyết định: tải lại chi tiết, giường trống và danh sách CRUD. Lỗi 409 (BED_TAKEN, trạng
 * thái đã đổi) do useApiErrorHandler báo câu tiếng Việt và tải lại dữ liệu đang hiển thị.
 */
function useTransferMutation<T>(
  id: number,
  mutationFn: (value: T) => Promise<unknown>,
) {
  const queryClient = useQueryClient()
  const onError = useApiErrorHandler()
  return useMutation({
    mutationFn,
    onSuccess: () => {
      // Chỉ chờ chi tiết đang xem; danh sách và giường trống tải lại nền, không giữ hộp thoại.
      void queryClient.invalidateQueries({ queryKey: ['room'] })
      void queryClient.invalidateQueries({ queryKey: ['crud'] })
      return queryClient.invalidateQueries({ queryKey: transferKeys.detail(id) })
    },
    onError,
  })
}

export function useApproveTransfer(id: number) {
  return useTransferMutation(id, (targetBedId: number) =>
    approveTransfer(id, targetBedId),
  )
}

export function useRejectTransfer(id: number) {
  return useTransferMutation(id, (reason: string) => rejectTransfer(id, reason))
}

export function useCancelTransfer(id: number) {
  return useTransferMutation(id, () => cancelTransfer(id))
}
