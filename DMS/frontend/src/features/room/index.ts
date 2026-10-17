// Feature room (TV2): hook TanStack Query cho chi tiết phòng, trưởng phòng và giường trống.
// Hàm gọi API của module đặt ở services/api/room.ts.
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { getLookupRecords } from '../crud/api'
import {
  assignRoomLeader,
  getAvailableBeds,
  getRoom,
  removeRoomLeader,
  type AvailableBedsParams,
} from '../../services/api/room'
import { useApiErrorHandler } from '../../hooks/useApiErrorHandler'

export { default as BedCrudPanel } from './BedCrudPanel'

export const roomKeys = {
  detail: (id: number) => ['room', id] as const,
  availableBeds: (params: AvailableBedsParams) =>
    ['room', 'available-beds', params] as const,
  catalog: (endpoint: string) => ['crud-lookup', endpoint] as const,
}

/** Danh mục tòa / loại phòng dùng cho bộ lọc và nhãn hiển thị. */
export function useRoomCatalog(endpoint: '/buildings' | '/room-types') {
  return useQuery({
    queryKey: roomKeys.catalog(endpoint),
    queryFn: () => getLookupRecords(endpoint),
    staleTime: 5 * 60 * 1000,
  })
}

export function useRoomDetail(id: number) {
  return useQuery({
    queryKey: roomKeys.detail(id),
    queryFn: () => getRoom(id),
    enabled: Number.isSafeInteger(id) && id > 0,
  })
}

/** Giường trống gom theo phòng (API 4.4); chỉ gọi khi có nguồn đơn hoặc yêu cầu. */
export function useAvailableBeds(params: AvailableBedsParams, enabled = true) {
  return useQuery({
    queryKey: roomKeys.availableBeds(params),
    queryFn: () => getAvailableBeds(params),
    enabled:
      enabled &&
      (params.applicationId != null || params.transferRequestId != null),
  })
}

function useRoomMutation<T>(
  id: number,
  mutationFn: (value: T) => Promise<unknown>,
) {
  const queryClient = useQueryClient()
  const onError = useApiErrorHandler()
  return useMutation({
    mutationFn,
    onSuccess: () => {
      // Chỉ chờ chi tiết phòng đang xem; danh sách CRUD tải lại nền.
      void queryClient.invalidateQueries({ queryKey: ['crud'] })
      return queryClient.invalidateQueries({ queryKey: roomKeys.detail(id) })
    },
    onError,
  })
}

export function useAssignLeader(id: number) {
  return useRoomMutation(id, (studentId: number) =>
    assignRoomLeader(id, studentId),
  )
}

export function useRemoveLeader(id: number) {
  return useRoomMutation(id, (reason: string) => removeRoomLeader(id, reason))
}
