import { api } from './client'

// API Spec 4.11: chi tiết ticket, thao tác xử lý và kết luận trừ cọc.

export interface MaintenancePhoto {
  id: number
  /** Đường dẫn ảnh đã kiểm tra quyền, ví dụ /api/maintenance-requests/1/photos/2. */
  url: string
}

export interface MaintenanceTimelineEntry {
  fromStatus: string | null
  toStatus: string
  actorId: number | null
  reason: string | null
  at: string
}

export interface MaintenanceStudentRef {
  id: number
  studentCode: string
  fullName: string
  gender?: string | null
}

export interface MaintenanceDeduction {
  contractId: number
  student: MaintenanceStudentRef | null
  amount: number
  topUpDueDate: string | null
}

export type MaintenanceCause = 'WEAR' | 'MISUSE'

export interface MaintenanceDetail {
  id: number
  code: string
  requesterId: number
  roomId: number | null
  buildingId: number | null
  floor: number | null
  category: string
  severity: string
  description: string
  status: string
  assignedTo: number | null
  createdAt: string
  photos: MaintenancePhoto[] | null
  allowedActions: string[] | null
  timeline: MaintenanceTimelineEntry[] | null
  /** Ba trường dưới chỉ có ở ticket đã kết luận theo kế hoạch TV4. */
  cause?: MaintenanceCause | null
  repairCost?: number | null
  costNote?: string | null
  concluded?: boolean | null
  deductions?: MaintenanceDeduction[] | null
}

export interface ChargeableStudent {
  student: MaintenanceStudentRef
  contractId: number
  contractStatus: string
}

export interface ConcludeMaintenanceRequest {
  cause: MaintenanceCause
  repairCost?: number
  chargedStudentIds?: number[]
  note?: string
}

/**
 * Body JSON của multipart `data` khi tạo/sửa ticket.
 * Tạo có `roomId` (phòng) HOẶC `buildingId` + `floor` (khu vực chung); sinh viên không gửi vị trí.
 */
export interface MaintenanceSaveInput {
  id?: number
  category: string
  severity: string
  description: string
  roomId?: number
  buildingId?: number
  floor?: number
}

/** Bộ lọc danh sách khớp SearchRequest; trang bắt đầu từ 0. */
export interface MaintenanceSearchRequest {
  page?: number
  size?: number
  q?: string
  sort?: string[]
  status?: string[]
  category?: string
  severity?: string
}

export interface TechnicianSummary {
  id: number
  fullName: string | null
  username: string
  taskCount?: number | null
}

export interface PageResponse<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

const base = '/maintenance-requests'

/** Ảnh trả về đường dẫn /api/... nhưng request cần Bearer JWT; gallery tải blob qua axios. */
export async function getMaintenancePhotoBlob(url: string): Promise<Blob> {
  const path = url.replace(/^\/api(?=\/)/, '')
  return (await api.get<Blob>(path, { responseType: 'blob' })).data
}

export async function getMaintenanceRequest(
  id: number,
): Promise<MaintenanceDetail> {
  return (await api.get<MaintenanceDetail>(`${base}/${id}`)).data
}

/**
 * Tạo/sửa ticket bằng multipart: phần `data` là JSON có ID khi sửa, `photos` gồm 0–3 ảnh.
 * Khi sửa backend không nhận ảnh mới, nên caller chỉ truyền `photos` ở nhánh tạo.
 */
export async function saveMaintenanceRequest(
  input: MaintenanceSaveInput,
  photos: File[] = [],
): Promise<MaintenanceDetail> {
  const form = new FormData()
  form.append(
    'data',
    new Blob([JSON.stringify(input)], { type: 'application/json' }),
  )
  for (const photo of photos) form.append('photos', photo)
  return (await api.post<MaintenanceDetail>(`${base}/save-or-update`, form))
    .data
}

/** Nhánh tạo mới không có ID; giữ tên riêng cho màn báo hỏng. */
export function createMaintenanceRequest(
  input: Omit<MaintenanceSaveInput, 'id'>,
  photos: File[] = [],
): Promise<MaintenanceDetail> {
  return saveMaintenanceRequest(input, photos)
}

export async function assignMaintenanceTechnician(
  id: number,
  technicianId: number,
): Promise<MaintenanceDetail> {
  return (
    await api.post<MaintenanceDetail>(`${base}/${id}/assign`, { technicianId })
  ).data
}

export async function rejectMaintenanceRequest(
  id: number,
  reason: string,
): Promise<MaintenanceDetail> {
  return (await api.post<MaintenanceDetail>(`${base}/${id}/reject`, { reason }))
    .data
}

export async function acceptMaintenanceRequest(
  id: number,
  reason?: string,
): Promise<MaintenanceDetail> {
  // ReasonRequest có @NotBlank: không gửi body rỗng khi sinh viên tự nghiệm thu.
  return (
    await api.post<MaintenanceDetail>(
      `${base}/${id}/accept`,
      reason ? { reason } : undefined,
    )
  ).data
}

export async function reopenMaintenanceRequest(
  id: number,
  reason: string,
): Promise<MaintenanceDetail> {
  return (await api.post<MaintenanceDetail>(`${base}/${id}/reopen`, { reason }))
    .data
}

export async function cancelMaintenanceRequest(
  id: number,
): Promise<MaintenanceDetail> {
  return (await api.post<MaintenanceDetail>(`${base}/${id}/cancel`)).data
}

export async function startMaintenanceRequest(
  id: number,
): Promise<MaintenanceDetail> {
  return (await api.post<MaintenanceDetail>(`${base}/${id}/start`)).data
}

export async function addMaintenanceNote(
  id: number,
  note: string,
): Promise<MaintenanceDetail> {
  return (await api.post<MaintenanceDetail>(`${base}/${id}/notes`, { note }))
    .data
}

export async function resolveMaintenanceRequest(
  id: number,
  note: string,
): Promise<MaintenanceDetail> {
  return (await api.post<MaintenanceDetail>(`${base}/${id}/resolve`, { note }))
    .data
}

export async function getChargeableStudents(
  id: number,
): Promise<ChargeableStudent[]> {
  return (await api.get<ChargeableStudent[]>(`${base}/${id}/chargeable-students`))
    .data
}

export async function concludeMaintenanceRequest(
  id: number,
  input: ConcludeMaintenanceRequest,
): Promise<MaintenanceDetail> {
  return (await api.post<MaintenanceDetail>(`${base}/${id}/conclude`, input))
    .data
}

/** Danh sách KTV đang hoạt động trong đơn vị; API chỉ cho tài khoản có quyền cập nhật ticket. */
export async function searchTechnicians(
  q = '',
): Promise<TechnicianSummary[]> {
  const page = (
    await api.post<PageResponse<TechnicianSummary>>(
      '/technicians/search-by-page',
      { page: 0, size: 100, q },
    )
  ).data
  return page.content ?? []
}
