// Feature maintenance (TV4): hook TanStack Query cho ticket sửa chữa.
// Hàm gọi API của module đặt ở services/api/maintenance.ts.
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useApiErrorHandler } from '../../hooks/useApiErrorHandler'
import {
  acceptMaintenanceRequest,
  addMaintenanceNote,
  assignMaintenanceTechnician,
  cancelMaintenanceRequest,
  concludeMaintenanceRequest,
  createMaintenanceRequest,
  getChargeableStudents,
  getMaintenanceRequest,
  rejectMaintenanceRequest,
  reopenMaintenanceRequest,
  resolveMaintenanceRequest,
  searchTechnicians,
  startMaintenanceRequest,
  type ConcludeMaintenanceRequest,
  type MaintenanceDetail,
  type MaintenanceSaveInput,
} from '../../services/api/maintenance'
import { getLookupRecords } from '../crud/api'

export const maintenanceKeys = {
  detail: (id: number) => ['maintenance', 'detail', id] as const,
  chargeableStudents: (id: number) =>
    ['maintenance', 'chargeable-students', id] as const,
  technicians: () => ['maintenance', 'technicians'] as const,
  rooms: () => ['maintenance', 'room-options'] as const,
  buildings: () => ['maintenance', 'building-options'] as const,
}

export function useMaintenanceDetail(id: number) {
  return useQuery({
    queryKey: maintenanceKeys.detail(id),
    queryFn: () => getMaintenanceRequest(id),
    enabled: Number.isSafeInteger(id) && id > 0,
  })
}

/** Ứng viên trừ cọc tại thời điểm tạo ticket; chỉ tải khi mở modal kết luận MISUSE. */
export function useChargeableStudents(id: number, enabled: boolean) {
  return useQuery({
    queryKey: maintenanceKeys.chargeableStudents(id),
    queryFn: () => getChargeableStudents(id),
    enabled: enabled && Number.isSafeInteger(id) && id > 0,
  })
}

export function useTechnicians(enabled: boolean) {
  return useQuery({
    queryKey: maintenanceKeys.technicians(),
    queryFn: () => searchTechnicians(),
    enabled,
    staleTime: 60 * 1000,
  })
}

/**
 * Danh mục phòng/tòa cho form Staff. Lookup tải đủ các trang quyền được xem
 * (getLookupRecords) để chọn tham chiếu; không phân trang lại dữ liệu ở React.
 */
export function useMaintenanceRooms(enabled: boolean) {
  return useQuery({
    queryKey: maintenanceKeys.rooms(),
    queryFn: () => getLookupRecords('/rooms'),
    enabled,
    staleTime: 5 * 60 * 1000,
  })
}

export function useMaintenanceBuildings(enabled: boolean) {
  return useQuery({
    queryKey: maintenanceKeys.buildings(),
    queryFn: () => getLookupRecords('/buildings'),
    enabled,
    staleTime: 5 * 60 * 1000,
  })
}

/**
 * Sau mỗi thao tác: tải lại chi tiết đang xem, danh sách CRUD, dashboard và sổ cọc
 * (kết luận MISUSE sinh giao dịch trừ cọc). Lỗi 409 do useApiErrorHandler báo và tải lại dữ liệu.
 */
function useTicketMutation<TInput>(
  id: number,
  mutationFn: (input: TInput) => Promise<MaintenanceDetail>,
  extraKeys: readonly (readonly unknown[])[] = [],
) {
  const queryClient = useQueryClient()
  const onError = useApiErrorHandler()
  return useMutation({
    mutationFn,
    onSuccess: (detail) => {
      // Hiển thị ngay response mới, đồng thời tải lại các màn phụ thuộc.
      queryClient.setQueryData(maintenanceKeys.detail(id), detail)
      void queryClient.invalidateQueries({ queryKey: ['crud'] })
      void queryClient.invalidateQueries({ queryKey: ['staff', 'dashboard'] })
      void queryClient.invalidateQueries({ queryKey: ['deposits'] })
      void queryClient.invalidateQueries({ queryKey: ['notification'] })
      for (const queryKey of extraKeys)
        void queryClient.invalidateQueries({ queryKey })
      return queryClient.invalidateQueries({
        queryKey: maintenanceKeys.detail(id),
      })
    },
    onError,
  })
}

export function useAssignTechnician(id: number) {
  return useTicketMutation(
    id,
    (technicianId: number) => assignMaintenanceTechnician(id, technicianId),
    [maintenanceKeys.technicians()],
  )
}

export function useRejectTicket(id: number) {
  return useTicketMutation(id, (reason: string) =>
    rejectMaintenanceRequest(id, reason),
  )
}

export function useAcceptTicket(id: number) {
  return useTicketMutation(id, (reason: string | undefined) =>
    acceptMaintenanceRequest(id, reason),
  )
}

export function useReopenTicket(id: number) {
  return useTicketMutation(id, (reason: string) =>
    reopenMaintenanceRequest(id, reason),
  )
}

export function useCancelTicket(id: number) {
  return useTicketMutation<void>(id, () => cancelMaintenanceRequest(id))
}

export function useStartTicket(id: number) {
  return useTicketMutation<void>(id, () => startMaintenanceRequest(id))
}

export function useAddTicketNote(id: number) {
  return useTicketMutation(id, (note: string) => addMaintenanceNote(id, note))
}

export function useResolveTicket(id: number) {
  return useTicketMutation(id, (note: string) =>
    resolveMaintenanceRequest(id, note),
  )
}

export function useConcludeTicket(id: number) {
  return useTicketMutation(
    id,
    (input: ConcludeMaintenanceRequest) => concludeMaintenanceRequest(id, input),
    [maintenanceKeys.chargeableStudents(id)],
  )
}

export interface CreateTicketInput {
  data: Omit<MaintenanceSaveInput, 'id'>
  photos: File[]
}

/** Tạo ticket mới (multipart); lỗi ảnh/CSDL do backend rollback toàn bộ. */
export function useCreateTicket() {
  const queryClient = useQueryClient()
  const onError = useApiErrorHandler()
  return useMutation({
    mutationFn: ({ data, photos }: CreateTicketInput) =>
      createMaintenanceRequest(data, photos),
    onSuccess: (detail) => {
      queryClient.setQueryData(maintenanceKeys.detail(detail.id), detail)
      void queryClient.invalidateQueries({ queryKey: ['crud'] })
      void queryClient.invalidateQueries({ queryKey: ['staff', 'dashboard'] })
      void queryClient.invalidateQueries({ queryKey: ['notification'] })
      return detail
    },
    onError,
  })
}
