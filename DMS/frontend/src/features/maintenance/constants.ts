import { crudValueLabels } from '../../constants/statusConfig'
import type { MaintenanceCause as MaintenanceCauseCode } from '../../services/api/maintenance'

// Mã hành động khớp DmsConstants.BusinessAction và allowedActions của backend (API Spec 4.11).
export const MaintenanceAction = {
  ASSIGN: 'assign',
  REJECT: 'reject',
  ACCEPT: 'accept',
  REOPEN: 'reopen',
  CANCEL: 'cancel',
  START: 'start',
  ADD_NOTE: 'add-note',
  RESOLVE: 'resolve',
  CONCLUDE: 'conclude',
} as const

export type MaintenanceAction =
  (typeof MaintenanceAction)[keyof typeof MaintenanceAction]

export const MaintenanceCause = {
  WEAR: 'WEAR',
  MISUSE: 'MISUSE',
} as const satisfies Record<MaintenanceCauseCode, MaintenanceCauseCode>

/** Luồng chuẩn của ticket sửa chữa; REJECTED/CANCELLED là nhánh kết thúc (SRS mục 10). */
export const maintenanceTicketFlow = [
  'OPEN',
  'ASSIGNED',
  'IN_PROGRESS',
  'RESOLVED',
  'CLOSED',
]

/** Mã hạng mục/mức độ đúng thứ tự form; nhãn đọc từ registry dùng chung statusConfig. */
export const maintenanceCategoryCodes = [
  'ELECTRIC',
  'WATER',
  'FURNITURE',
  'DOOR_LOCK',
  'OTHER',
] as const

export const maintenanceSeverityCodes = ['LOW', 'MEDIUM', 'HIGH'] as const

export const maintenanceCategoryOptions = maintenanceCategoryCodes.map(
  (value) => ({ value, label: crudValueLabels[value] ?? value }),
)

export const maintenanceSeverityOptions = maintenanceSeverityCodes.map(
  (value) => ({ value, label: crudValueLabels[value] ?? value }),
)

// Nguyên nhân kết luận chưa có trong registry; main agent chuyển sang statusConfig.ts khi tích hợp.
export const maintenanceCauseLabels: Record<MaintenanceCauseCode, string> = {
  WEAR: 'Hao mòn tự nhiên',
  MISUSE: 'Do sử dụng sai',
}

export const maintenanceActionLabels: Record<string, string> = {
  [MaintenanceAction.ASSIGN]: 'Giao kỹ thuật viên',
  [MaintenanceAction.REJECT]: 'Từ chối',
  [MaintenanceAction.ACCEPT]: 'Nghiệm thu',
  [MaintenanceAction.REOPEN]: 'Mở lại',
  [MaintenanceAction.CANCEL]: 'Hủy ticket',
  [MaintenanceAction.START]: 'Bắt đầu sửa chữa',
  [MaintenanceAction.ADD_NOTE]: 'Ghi chú tiến độ',
  [MaintenanceAction.RESOLVE]: 'Hoàn thành sửa chữa',
  [MaintenanceAction.CONCLUDE]: 'Kết luận xử lý',
}

/** Giới hạn ảnh đúng SRS/SAD: tối đa 3 ảnh JPEG/PNG/WebP, mỗi ảnh ≤ 5 MB. */
export const maintenancePhotoRules = {
  maxFiles: 3,
  maxBytes: 5 * 1024 * 1024,
  acceptedTypes: ['image/jpeg', 'image/png', 'image/webp'],
} as const

export function maintenanceLabel(
  labels: Record<string, string>,
  value: string | null | undefined,
): string {
  return value ? (labels[value] ?? value) : '—'
}

/** Nhãn hạng mục/mức độ dùng registry chung thay vì tạo bản sao trong màn hình. */
export function maintenanceValueLabel(value: string | null | undefined) {
  return maintenanceLabel(crudValueLabels, value)
}
