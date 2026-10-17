import { Role } from './authorization'
import {
  CheckCircleOutlined,
  ClockCircleOutlined,
  CloseCircleOutlined,
  EditOutlined,
  FileSearchOutlined,
  InboxOutlined,
  LockOutlined,
  MinusCircleOutlined,
  PlayCircleOutlined,
  SafetyCertificateOutlined,
  SendOutlined,
  StopOutlined,
  ToolOutlined,
  UserSwitchOutlined,
  WarningOutlined,
} from '@ant-design/icons'
import type { ComponentType } from 'react'

// Nguồn duy nhất cho nhãn, màu, icon của mọi trạng thái (UI/UX mục 2.2).
// Người dùng không bao giờ thấy enum tiếng Anh. Khóa theo (entity, status) vì một enum
// có thể mang nghĩa khác nhau: ASSIGNED ở đơn là "Đã giữ chỗ", ở ticket là "Đã giao KTV".

// ---- Lớp 1: trạng thái lưu trong CSDL → badge đặc -------------------------------------------

export type StatusColor =
  'gray' | 'blue' | 'cyan' | 'purple' | 'orange' | 'green' | 'red'

/** Nền đặc, chữ trắng; mọi mã màu đạt tương phản ≥ 4.5:1 (UI/UX 2.1a). */
export const statusColors: Record<StatusColor, string> = {
  gray: '#4B5563',
  blue: '#1D4ED8',
  cyan: '#0E7490',
  purple: '#7E22CE',
  orange: '#B45309',
  green: '#15803D',
  red: '#B91C1C',
}

type IconType = ComponentType<{ 'aria-hidden'?: boolean }>

export interface StatusStyle {
  label: string
  color: StatusColor
  icon: IconType
}

export type StatusEntity =
  'application' | 'contract' | 'ticket' | 'invoice' | 'request'

const s = (label: string, color: StatusColor, icon: IconType): StatusStyle => ({
  label,
  color,
  icon,
})

export const statusConfig: Record<StatusEntity, Record<string, StatusStyle>> = {
  application: {
    DRAFT: s('Nháp', 'gray', EditOutlined),
    SUBMITTED: s('Đã gửi', 'blue', SendOutlined),
    APPROVED: s('Đã duyệt', 'cyan', CheckCircleOutlined),
    ASSIGNED: s('Đã giữ chỗ', 'purple', LockOutlined),
    REJECTED: s('Bị từ chối', 'red', CloseCircleOutlined),
    CANCELLED: s('Đã hủy', 'gray', StopOutlined),
  },
  contract: {
    PENDING: s('Chờ kích hoạt', 'orange', ClockCircleOutlined),
    ACTIVE: s('Đang hiệu lực', 'green', CheckCircleOutlined),
    EXPIRED: s('Đã kết thúc', 'gray', MinusCircleOutlined),
    TERMINATED: s('Chấm dứt trước hạn', 'gray', MinusCircleOutlined),
    CANCELLED: s('Đã hủy', 'gray', StopOutlined),
  },
  ticket: {
    OPEN: s('Chờ xử lý', 'blue', ClockCircleOutlined),
    ASSIGNED: s('Đã giao KTV', 'purple', UserSwitchOutlined),
    IN_PROGRESS: s('Đang sửa', 'orange', ToolOutlined),
    RESOLVED: s('Chờ nghiệm thu', 'cyan', FileSearchOutlined),
    CLOSED: s('Đã đóng', 'green', SafetyCertificateOutlined),
    REJECTED: s('Bị từ chối', 'red', CloseCircleOutlined),
    CANCELLED: s('Đã hủy', 'gray', StopOutlined),
  },
  invoice: {
    UNPAID: s('Chưa thanh toán', 'orange', ClockCircleOutlined),
    PAID: s('Đã thanh toán', 'green', CheckCircleOutlined),
  },
  // Yêu cầu gia hạn, chuyển phòng, lựa chọn hè
  request: {
    PENDING: s('Chờ duyệt', 'orange', ClockCircleOutlined),
    APPROVED: s('Đã duyệt', 'green', CheckCircleOutlined),
    REJECTED: s('Bị từ chối', 'red', CloseCircleOutlined),
    CANCELLED: s('Đã hủy', 'gray', StopOutlined),
  },
}

const unknownStatus = (status: string): StatusStyle => ({
  label: status,
  color: 'gray',
  icon: PlayCircleOutlined,
})

export function getStatus(entity: StatusEntity, status: string): StatusStyle {
  const found = statusConfig[entity]?.[status]
  if (found) return found
  if (import.meta.env.DEV)
    console.warn(`statusConfig thiếu (${entity}, ${status})`)
  return unknownStatus(status)
}

// ---- Lớp 2: nhãn suy ra → chip viền + icon, đặt cạnh badge, không thay thế nó ---------------
// Không lưu trong CSDL; backend tính (BR-08) và trả về mã trong `labels` của hợp đồng (API Spec mục 6), nên khóa
// của hợp đồng trùng mã API. Frontend không tự tính ngưỡng (ngưỡng nằm ở system_config).

export type DerivedTone = 'warning' | 'danger' | 'neutral'

export const derivedToneColors: Record<DerivedTone, string> = {
  warning: '#B45309',
  danger: '#B91C1C',
  neutral: '#4B5563',
}

export interface DerivedStyle {
  label: string
  tone: DerivedTone
  icon: IconType
}

export type DerivedLabel =
  | 'EXPIRING'
  | 'OVERDUE_MOVE_OUT'
  | 'SUMMER_LEAVE'
  | 'INVOICE_OVERDUE'
  | 'TOPUP_OVERDUE'

export const derivedLabels: Record<DerivedLabel, DerivedStyle> = {
  EXPIRING: { label: 'Sắp hết hạn', tone: 'warning', icon: WarningOutlined },
  OVERDUE_MOVE_OUT: {
    label: 'Quá hạn trả phòng',
    tone: 'danger',
    icon: WarningOutlined,
  },
  SUMMER_LEAVE: { label: 'Tạm rời hè', tone: 'neutral', icon: InboxOutlined },
  INVOICE_OVERDUE: { label: 'Quá hạn', tone: 'danger', icon: WarningOutlined },
  TOPUP_OVERDUE: {
    label: 'Quá hạn bù cọc',
    tone: 'danger',
    icon: WarningOutlined,
  },
}

// ---- Lớp 3: phòng và giường → hai thuộc tính tách biệt --------------------------------------

export const roomOperation: Record<
  string,
  { label: string; color: StatusColor }
> = {
  ACTIVE: { label: 'Hoạt động', color: 'green' },
  MAINTENANCE: { label: 'Bảo trì', color: 'orange' },
  INACTIVE: { label: 'Ngừng sử dụng', color: 'gray' },
  CLOSED: { label: 'Đóng', color: 'gray' },
}

/** Giới tính của phòng (room.gender). */
export const roomGender: Record<string, string> = {
  M: 'Nam',
  F: 'Nữ',
}

export const bedOperation: Record<string, { label: string }> = {
  ACTIVE: { label: 'Hoạt động' },
  BROKEN: { label: 'Hỏng' },
  RETIRED: { label: 'Ngừng dùng' },
}

/** Chiếm dụng giường (tính ra, không lưu): màu nền ô giường trắng / vàng nhạt / xanh. */
export type BedOccupancy = 'FREE' | 'HELD' | 'OCCUPIED'

export const bedOccupancy: Record<
  BedOccupancy,
  { label: string; background: string; border: string }
> = {
  FREE: { label: 'Trống', background: '#FFFFFF', border: '#D1D5DB' },
  HELD: { label: 'Đang giữ', background: '#FEF3C7', border: '#B45309' },
  OCCUPIED: { label: 'Đang ở', background: '#DCFCE7', border: '#15803D' },
}

/** Loại phân phòng mở → chiếm dụng giường: giữ chỗ (HOLD) hoặc đang ở (STAY). */
export const assignmentOccupancy: Record<'HOLD' | 'STAY', BedOccupancy> = {
  HOLD: 'HELD',
  STAY: 'OCCUPIED',
}

/** Loại phân phòng mở của lịch sử phân giường (khớp `room_assignment_history.kind`). */
export type AssignmentKind = keyof typeof assignmentOccupancy

/** Nhãn chiếm dụng giường (Đang giữ / Đang ở) theo loại phân phòng mở. */
export function assignmentLabel(kind: AssignmentKind) {
  return bedOccupancy[assignmentOccupancy[kind]].label
}

/** Trạng thái vận hành giường dùng ngoài bảng nhãn (khớp `bed.status`). */
export const BED_STATUS = { BROKEN: 'BROKEN' } as const

/** Mã hành động của yêu cầu chuyển phòng trong `allowedActions` (khớp API-Spec mục 4.7). */
export const TRANSFER_ACTION = {
  APPROVE: 'approve',
  REJECT: 'reject',
  CANCEL: 'cancel',
} as const

/** Nhãn giá trị dùng chung cho bảng/form CRUD; trang nghiệp vụ không tạo bản sao riêng. */
export const crudValueLabels: Record<string, string> = {
  ACTIVE: 'Hoạt động',
  BROKEN: 'Hỏng',
  RETIRED: 'Ngừng dùng',
  CLOSED: 'Đã đóng',
  MAINTENANCE: 'Bảo trì',
  DRAFT: 'Nháp',
  SUBMITTED: 'Đã nộp',
  APPROVED: 'Đã duyệt',
  REJECTED: 'Từ chối',
  CANCELLED: 'Đã hủy',
  PENDING: 'Chờ xử lý',
  UNPAID: 'Chưa thanh toán',
  PAID: 'Đã thanh toán',
  M: 'Nam',
  F: 'Nữ',
  [Role.STAFF]: 'Quản lý',
  [Role.ADMIN]: 'Quản trị',
  [Role.STUDENT]: 'Sinh viên',
  [Role.TECHNICIAN]: 'Kỹ thuật viên',
  ROOM_FEE: 'Phí phòng',
  UTILITY: 'Điện nước',
  ELECTRIC: 'Điện',
  WATER: 'Nước',
  LOW: 'Thấp',
  MEDIUM: 'Vừa',
  HIGH: 'Cao',
  OPEN: 'Mở',
  FURNITURE: 'Nội thất',
  DOOR_LOCK: 'Cửa/khóa',
  OTHER: 'Khác',
  STAY: 'Ở hè',
  TEMP_LEAVE: 'Tạm rời hè',
  LEAVE: 'Rời KTX',
}
