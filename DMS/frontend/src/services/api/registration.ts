import { api } from './client'

// API Spec 4.3–4.4: chi tiết đơn đăng ký, phân giường, giải phóng giữ chỗ và xác nhận diện ưu tiên.
// Trang chi tiết (features/registration) chỉ gọi các hàm ở đây; không gọi axios trực tiếp.

/** Trạng thái đơn khớp DmsConstants.ApplicationStatus. */
export const ApplicationStatus = {
  DRAFT: 'DRAFT',
  SUBMITTED: 'SUBMITTED',
  APPROVED: 'APPROVED',
  ASSIGNED: 'ASSIGNED',
  REJECTED: 'REJECTED',
  CANCELLED: 'CANCELLED',
} as const

export type ApplicationStatus =
  (typeof ApplicationStatus)[keyof typeof ApplicationStatus]

/** Mã hành động trong `allowedActions` (API Spec mục 6); trùng đoạn cuối URL backend. */
export const ApplicationAction = {
  EDIT: 'edit',
  SUBMIT: 'submit',
  APPROVE: 'approve',
  REJECT: 'reject',
  CANCEL: 'cancel',
  ASSIGN: 'assign',
  RELEASE_HOLD: 'release-hold',
  VERIFY_PRIORITY: 'verify-priority',
} as const

export type ApplicationAction =
  (typeof ApplicationAction)[keyof typeof ApplicationAction]

/** Mã lỗi cần rẽ nhánh trên màn chi tiết đơn (câu tiếng Việt ở utils/errorMessages). */
export const RegistrationErrorCode = {
  BED_TAKEN: 'BED_TAKEN',
  INVALID_STATE_TRANSITION: 'INVALID_STATE_TRANSITION',
} as const

export interface ApplicationPersonRef {
  id: number
  fullName: string | null
}

export interface ApplicationStudent {
  id: number
  studentCode: string
  fullName: string
  gender?: string | null
}

/** Đợt/đơn vị/loại phòng dạng tóm tắt khi backend trả kèm chi tiết. */
export interface ApplicationRef {
  id: number
  code?: string | null
  name?: string | null
  semesterId?: number | null
}

/**
 * Diện ưu tiên của đơn. `verified`: null = chưa xác nhận, true = đúng, false = không đúng.
 * Điểm ưu tiên là tổng trọng số các diện chưa bị xác nhận "không đúng" (backend tính).
 */
export interface ApplicationPriority {
  categoryId: number
  name: string
  weight: number
  verified: boolean | null
  note?: string | null
  /** ID người xác nhận; tên nằm ở `verifiedByName` (ApplicationDtos.Priority). */
  verifiedBy?: number | null
  verifiedByName?: string | null
}

export interface ApplicationTimelineEntry {
  fromStatus: string | null
  toStatus: string
  actorId: number | null
  /** Một số phản hồi kèm actor tóm tắt; timeline hiện tại chỉ có actorId. */
  actor?: ApplicationPersonRef | null
  reason: string | null
  at: string
}

export interface ApplicationBedRef {
  /** Backend phẳng có thể chỉ gửi `bedId`; giữ tùy chọn cho dạng lồng cũ. */
  id?: number | null
  /** Mã đầy đủ do backend ghép (K1-A101-03) hoặc mã ngắn trong phòng. */
  code: string
  roomId?: number | null
  roomCode?: string | null
  buildingId?: number | null
  buildingCode?: string | null
  floor?: number | null
  unitId?: number | null
}

/** Giữ chỗ + hợp đồng PENDING sinh ra khi phân giường; chỉ có ở trạng thái ASSIGNED. */
export interface ApplicationHold {
  assignmentId?: number | null
  /** Backend phẳng trả `bedId`/`bedCode` rời; vẫn đọc object `bed` lồng của phản hồi cũ. */
  bed?: ApplicationBedRef | null
  bedId?: number | null
  bedCode?: string | null
  roomId?: number | null
  roomCode?: string | null
  buildingId?: number | null
  buildingCode?: string | null
  unitId?: number | null
  contractId?: number | null
  contractCode?: string | null
  activationDeadline?: string | null
}

export interface ApplicationDetail {
  id: number
  status: string
  student: ApplicationStudent | null
  periodId: number | null
  preferredUnitId: number | null
  preferredRoomTypeId: number | null
  priorityScore: number | null
  note: string | null
  rejectReason: string | null
  submittedAt: string | null
  priorities: ApplicationPriority[] | null
  allowedActions: string[] | null
  timeline: ApplicationTimelineEntry[] | null
  /** Các trường mở rộng tùy backend; frontend dùng khi có để tránh gọi lookup. */
  period?: ApplicationRef | null
  preferredUnit?: ApplicationRef | null
  preferredRoomType?: ApplicationRef | null
  hold?: ApplicationHold | null
  contractId?: number | null
  activationDeadline?: string | null
}

export interface VerifyPriorityRequest {
  verified: boolean
  note?: string
}

const base = '/applications'

export async function getApplication(id: number): Promise<ApplicationDetail> {
  return (await api.get<ApplicationDetail>(`${base}/${id}`)).data
}

export async function submitApplication(
  id: number,
): Promise<ApplicationDetail> {
  return (await api.post<ApplicationDetail>(`${base}/${id}/submit`)).data
}

export async function approveApplication(
  id: number,
): Promise<ApplicationDetail> {
  return (await api.post<ApplicationDetail>(`${base}/${id}/approve`)).data
}

export async function rejectApplication(
  id: number,
  reason: string,
): Promise<ApplicationDetail> {
  return (
    await api.post<ApplicationDetail>(`${base}/${id}/reject`, { reason })
  ).data
}

export async function cancelApplication(
  id: number,
): Promise<ApplicationDetail> {
  return (await api.post<ApplicationDetail>(`${base}/${id}/cancel`)).data
}

/** Phân giường APPROVED → ASSIGNED; backend tạo giữ chỗ HOLD và hợp đồng PENDING. */
export async function assignApplicationBed(
  id: number,
  bedId: number,
): Promise<ApplicationDetail> {
  return (
    await api.post<ApplicationDetail>(`${base}/${id}/assign`, { bedId })
  ).data
}

/** Hủy giữ chỗ ASSIGNED → APPROVED kèm lý do bắt buộc. */
export async function releaseApplicationHold(
  id: number,
  reason: string,
): Promise<ApplicationDetail> {
  return (
    await api.post<ApplicationDetail>(`${base}/${id}/release-hold`, { reason })
  ).data
}

/** Xác nhận/từ chối một diện ưu tiên; backend tính lại điểm và trả chi tiết mới. */
export async function verifyApplicationPriority(
  id: number,
  categoryId: number,
  input: VerifyPriorityRequest,
): Promise<ApplicationDetail> {
  return (
    await api.post<ApplicationDetail>(
      `${base}/${id}/priorities/${categoryId}/verify`,
      input,
    )
  ).data
}

/** Giường đang giữ: đọc object `bed` lồng lẫn các trường phẳng `bedId`/`bedCode` backend trả. */
export function applicationHoldBed(
  hold: ApplicationHold | null | undefined,
): ApplicationBedRef | null {
  if (!hold) return null
  if (hold.bed) return hold.bed
  if (hold.bedId == null && !hold.bedCode) return null
  return {
    id: hold.bedId ?? null,
    code: hold.bedCode ?? '',
    roomId: hold.roomId ?? null,
    roomCode: hold.roomCode ?? null,
    buildingId: hold.buildingId ?? null,
    buildingCode: hold.buildingCode ?? null,
    unitId: hold.unitId ?? null,
  }
}
