import { crudValueLabels } from '../../constants/statusConfig'
import type { MaintenanceCause as MaintenanceCauseCode } from '../../services/api/maintenance'

// Mã hành động và nhãn khai một lần trong statusConfig.ts; re-export để feature dùng chung một nguồn.
export {
  MaintenanceAction,
  maintenanceActionLabels,
  maintenanceCauseLabels,
} from '../../constants/statusConfig'

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
