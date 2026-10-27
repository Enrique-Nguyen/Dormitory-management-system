// Feature contract (TV4): hook TanStack Query cho trả phòng và gửi/nhận đồ.
// Hàm gọi API nằm ở services/api/checkout.ts; không gọi axios trực tiếp trong component.
import {
  keepPreviousData,
  useMutation,
  useQuery,
  useQueryClient,
  type QueryClient,
} from '@tanstack/react-query'
import { useApiErrorHandler } from '../../hooks/useApiErrorHandler'
import {
  createStorageRecord,
  executeMoveOut,
  getMoveOutPreview,
  returnStorageRecord,
  searchStorageRecords,
  type CreateStorageRequest,
  type MoveOutRequest,
  type ReturnStorageRequest,
  type StorageSearchRequest,
} from '../../services/api/checkout'
import {
  getAvailableBeds,
  type AvailableBedsParams,
} from '../../services/api/room'
import { roomKeys } from '../room'

export const checkoutKeys = {
  moveOutPreview: (contractId: number, moveOutDate: string) =>
    ['contracts', contractId, 'move-out-preview', moveOutDate] as const,
  storageList: (request: StorageSearchRequest) =>
    ['storage-records', 'list', request] as const,
}

/**
 * Trả phòng đổi trạng thái hợp đồng, phân phòng, trưởng phòng, sổ cọc và Dashboard; nhận lại đồ
 * đổi phân phòng. Làm mới nền các nhóm dữ liệu liên quan, không giữ hộp thoại.
 */
function invalidateCheckoutData(queryClient: QueryClient) {
  for (const queryKey of [
    ['contracts'],
    ['contract'],
    ['crud'],
    ['room'],
    ['deposits'],
    ['staff'],
    ['notification'],
    ['notifications'],
  ]) {
    void queryClient.invalidateQueries({ queryKey })
  }
}

/** Giường trống cần khóa theo phiếu gửi đồ để backend xác định đúng đơn vị/giới tính. */
interface StorageBedsParams extends AvailableBedsParams {
  storageRecordId?: number
}

export interface StorageBedFilter {
  buildingId?: number
  roomTypeId?: number
}

/**
 * Chỉ gọi preview khi hộp thoại đã mở, có hợp đồng và ngày trả phòng hợp lệ. Đổi ngày tạo query
 * key mới nên dữ liệu cũ không bị dùng nhầm để xác nhận.
 */
export function useMoveOutPreview(
  contractId: number | undefined,
  moveOutDate: string | undefined,
  enabled = true,
) {
  return useQuery({
    queryKey: checkoutKeys.moveOutPreview(
      contractId ?? 0,
      moveOutDate ?? '',
    ),
    queryFn: () => getMoveOutPreview(contractId!, moveOutDate!),
    enabled:
      enabled &&
      Number.isSafeInteger(contractId) &&
      (contractId ?? 0) > 0 &&
      typeof moveOutDate === 'string' &&
      /^\d{4}-\d{2}-\d{2}$/.test(moveOutDate),
  })
}

interface ExecuteMoveOutInput extends MoveOutRequest {
  contractId: number
}

export function useExecuteMoveOut() {
  const queryClient = useQueryClient()
  const onError = useApiErrorHandler()
  return useMutation({
    mutationFn: ({ contractId, ...input }: ExecuteMoveOutInput) =>
      executeMoveOut(contractId, input),
    onSuccess: (result, input) => {
      void queryClient.invalidateQueries({
        queryKey: checkoutKeys.moveOutPreview(
          input.contractId,
          input.moveOutDate,
        ),
      })
      invalidateCheckoutData(queryClient)
      return result
    },
    onError,
  })
}

export function useStorageRecords(
  request: StorageSearchRequest,
  enabled = true,
) {
  return useQuery({
    queryKey: checkoutKeys.storageList(request),
    queryFn: () => searchStorageRecords(request),
    enabled,
    placeholderData: keepPreviousData,
  })
}

export function useCreateStorageRecord() {
  const queryClient = useQueryClient()
  const onError = useApiErrorHandler()
  return useMutation({
    mutationFn: (input: CreateStorageRequest & { contractId: number }) => {
      const { contractId, ...request } = input
      return createStorageRecord(contractId, request)
    },
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['storage-records'] })
      invalidateCheckoutData(queryClient)
    },
    onError,
  })
}

export function useReturnStorageRecord() {
  const queryClient = useQueryClient()
  const onError = useApiErrorHandler()
  return useMutation({
    mutationFn: (input: ReturnStorageRequest & { storageRecordId: number }) => {
      const { storageRecordId, ...request } = input
      return returnStorageRecord(storageRecordId, request)
    },
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['storage-records'] })
      invalidateCheckoutData(queryClient)
    },
    onError,
  })
}

/**
 * Giường trống cho phiếu gửi đồ. Backend hiện cần nhận `storageRecordId` để suy ra đơn vị/giới
 * tính của sinh viên; bộ lọc tòa/loại phòng chỉ thu hẹp kết quả. Khi backend chưa hỗ trợ tham
 * số này, main cần bổ sung ở `/beds/available` (xem ghi chú bàn giao).
 */
export function useStorageAvailableBeds(
  storageRecordId: number | undefined,
  filter: StorageBedFilter,
  enabled = true,
) {
  const validId =
    Number.isSafeInteger(storageRecordId) && (storageRecordId ?? 0) > 0
  const params: StorageBedsParams = {
    storageRecordId: validId ? storageRecordId : undefined,
    buildingId: filter.buildingId,
    roomTypeId: filter.roomTypeId,
  }
  return useQuery({
    queryKey: roomKeys.availableBeds(params),
    queryFn: () => getAvailableBeds(params),
    enabled: enabled && validId,
    placeholderData: keepPreviousData,
  })
}
