// Feature registration (TV3): hook TanStack Query cho chi tiết đơn, phân giường và diện ưu tiên.
// Hàm gọi API của module đặt ở services/api/registration.ts; component không gọi axios trực tiếp.
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useApiErrorHandler } from '../../hooks/useApiErrorHandler'
import {
  approveApplication,
  assignApplicationBed,
  cancelApplication,
  getApplication,
  rejectApplication,
  releaseApplicationHold,
  submitApplication,
  verifyApplicationPriority,
  type ApplicationDetail,
  type VerifyPriorityRequest,
} from '../../services/api/registration'
import { getLookupRecords } from '../crud/api'
import type { CrudRecord } from '../crud/types'

export const registrationKeys = {
  detail: (id: number) => ['applications', 'detail', id] as const,
  /** Cùng khóa với lookup của CrudPage để một lần làm mới cập nhật mọi màn. */
  catalog: (endpoint: string) => ['crud-lookup', endpoint] as const,
}

export function useApplicationDetail(id: number) {
  return useQuery({
    queryKey: registrationKeys.detail(id),
    queryFn: () => getApplication(id),
    enabled: Number.isSafeInteger(id) && id > 0,
  })
}

/**
 * Danh mục tòa/loại phòng cho bộ lọc BedPicker khi phân giường. Dùng cùng khóa cache với
 * features/room nên dữ liệu được chia sẻ và một lần làm mới cập nhật cả hai nơi.
 */
export function useAssignCatalog(enabled: boolean) {
  const buildings = useQuery({
    queryKey: registrationKeys.catalog('/buildings'),
    queryFn: () => getLookupRecords('/buildings'),
    enabled,
    staleTime: 5 * 60 * 1000,
  })
  const roomTypes = useQuery({
    queryKey: registrationKeys.catalog('/room-types'),
    queryFn: () => getLookupRecords('/room-types'),
    enabled,
    staleTime: 5 * 60 * 1000,
  })
  return { buildings, roomTypes }
}

/**
 * Nhãn đợt/đơn vị/loại phòng khi backend chỉ trả ID. Chỉ tải khi chi tiết thiếu tên mở rộng;
 * cả ba lookup đều là dữ liệu đọc mà Sinh viên và Quản lý đã có quyền.
 */
export function useApplicationCatalog(enabled: boolean) {
  const periods = useQuery({
    queryKey: registrationKeys.catalog('/registration-periods'),
    queryFn: () => getLookupRecords('/registration-periods'),
    enabled,
    staleTime: 5 * 60 * 1000,
  })
  const units = useQuery({
    queryKey: registrationKeys.catalog('/management-units'),
    queryFn: () => getLookupRecords('/management-units'),
    enabled,
    staleTime: 5 * 60 * 1000,
  })
  const roomTypes = useQuery({
    queryKey: registrationKeys.catalog('/room-types'),
    queryFn: () => getLookupRecords('/room-types'),
    enabled,
    staleTime: 5 * 60 * 1000,
  })
  return { periods, units, roomTypes }
}

/** Tên bản ghi lookup theo ID; trả về `fallback` khi đã có tên từ chi tiết. */
export function lookupName(
  records: CrudRecord[] | undefined,
  id: number | null | undefined,
  fallback?: string | null,
): string {
  if (fallback) return fallback
  if (id == null) return '—'
  const record = records?.find((item) => Number(item.id) === id)
  const name = record?.name ?? record?.code
  return name == null || name === '' ? `#${id}` : String(name)
}

/**
 * Sau mỗi thao tác: hiển thị ngay chi tiết mới, đồng thời tải lại danh sách CRUD, giường trống,
 * hợp đồng và thông báo. Lỗi 409 (BED_TAKEN, trạng thái đã đổi) do useApiErrorHandler báo và
 * làm mới dữ liệu đang xem.
 */
function useApplicationMutation<T>(
  id: number,
  mutationFn: (input: T) => Promise<ApplicationDetail>,
) {
  const queryClient = useQueryClient()
  const onError = useApiErrorHandler()
  return useMutation({
    mutationFn,
    onSuccess: (detail) => {
      queryClient.setQueryData(registrationKeys.detail(id), detail)
      for (const queryKey of [
        ['crud'],
        ['room'],
        ['contract'],
        ['contracts'],
        ['staff'],
        ['notifications'],
      ])
        void queryClient.invalidateQueries({ queryKey })
      return queryClient.invalidateQueries({
        queryKey: registrationKeys.detail(id),
      })
    },
    onError,
  })
}

export function useSubmitApplication(id: number) {
  return useApplicationMutation<void>(id, () => submitApplication(id))
}

export function useApproveApplication(id: number) {
  return useApplicationMutation<void>(id, () => approveApplication(id))
}

export function useRejectApplication(id: number) {
  return useApplicationMutation(id, (reason: string) =>
    rejectApplication(id, reason),
  )
}

export function useCancelApplication(id: number) {
  return useApplicationMutation<void>(id, () => cancelApplication(id))
}

export function useAssignApplicationBed(id: number) {
  return useApplicationMutation(id, (bedId: number) =>
    assignApplicationBed(id, bedId),
  )
}

export function useReleaseApplicationHold(id: number) {
  return useApplicationMutation(id, (reason: string) =>
    releaseApplicationHold(id, reason),
  )
}

export interface VerifyPriorityInput extends VerifyPriorityRequest {
  categoryId: number
}

export function useVerifyApplicationPriority(id: number) {
  return useApplicationMutation(
    id,
    ({ categoryId, ...input }: VerifyPriorityInput) =>
      verifyApplicationPriority(id, categoryId, input),
  )
}
