import { api } from './client'

// API Spec 4.8–4.9: trả phòng, phiếu gửi đồ tạm rời hè và nhận lại đồ.

/** Trang dữ liệu dùng chung cho các endpoint POST search-by-page. */
export interface CheckoutPage<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

/** Mã tình trạng tài sản khi trả phòng, dùng chung cho form và payload. */
export const AssetCondition = {
  OK: 'OK',
  DAMAGED: 'DAMAGED',
} as const

export type AssetCondition = (typeof AssetCondition)[keyof typeof AssetCondition]

export interface MoveOutInvoice {
  id: number
  code: string
  amount: number
  dueDate: string | null
}

export interface MoveOutTicket {
  id: number
  code: string
  status: string
  concluded: boolean
}

/**
 * Dữ liệu xem trước trả phòng. Backend hiện trả `warnings` dạng cờ boolean khi còn hóa đơn chưa
 * thanh toán hoặc ticket chưa kết luận; giao diện tự dựng câu cảnh báo từ các danh sách kèm theo.
 * Tên danh sách hóa đơn điện nước được đọc tolerant vì API Spec dự kiến dùng tên ngắn hơn.
 */
export interface MoveOutPreview {
  endStatus: string
  depositBalance: number
  refundAmount: number
  negativeBalanceDebt: number
  unpaidRoomFeeInvoices: MoveOutInvoice[]
  /** Tên backend đang dùng. */
  unpaidRoomUtilityInvoices?: MoveOutInvoice[] | null
  /** Tên dự kiến trong API Spec; chấp nhận cả hai để không vỡ khi backend đổi tên. */
  unpaidUtilityInvoices?: MoveOutInvoice[] | null
  unsettledTickets: MoveOutTicket[]
  isRoomLeader: boolean
  warnings?: boolean | string[] | null
}

/** Danh sách hóa đơn điện nước chưa thanh toán, đọc được cả hai tên trường. */
export function moveOutUtilityInvoices(
  preview: MoveOutPreview,
): MoveOutInvoice[] {
  return (
    preview.unpaidRoomUtilityInvoices ??
    preview.unpaidUtilityInvoices ??
    []
  )
}

export interface MoveOutRequest {
  moveOutDate: string
  assetCondition: AssetCondition
  assetNote?: string
  acknowledgedWarnings: boolean
}

/** Kết quả trả phòng: biên bản và số liệu cọc đã chốt trong cùng giao dịch. */
export interface MoveOutResult {
  moveOutRecordId: number
  contractId: number
  contractCode: string
  contractStatus: string
  moveOutDate: string
  endStatus: string
  depositBalance: number
  refundAmount: number
  negativeBalanceDebt: number
  unpaidRoomFee: number
  assetCondition: AssetCondition
  assetNote: string | null
  confirmedBy: number
  movedOutAt: string
}

/**
 * Phiếu gửi đồ trả về từ danh sách hoặc sau khi tạo/nhận lại. Danh sách bổ sung tên sinh viên và
 * mã hợp đồng; endpoint tạo/nhận lại chỉ trả các cột của phiếu.
 */
export interface StorageRecordItem {
  id: number
  contractId: number
  studentId?: number
  contractCode?: string | null
  fullName?: string | null
  summerChoiceId?: number | null
  previousBedId?: number | null
  packageCount: number
  description: string
  location: string
  depositedAt: string
  receivedBy?: number | null
  returnedAt?: string | null
  returnedBy?: number | null
}

export interface StorageSearchRequest {
  /** Trang bắt đầu từ 0 theo SearchRequest của backend. */
  page: number
  size: number
  q?: string
}

export interface CreateStorageRequest {
  packageCount: number
  description: string
  location: string
  depositedAt: string
}

export interface ReturnStorageRequest {
  bedId: number
  returnedAt: string
}

/** Xem trước công nợ, cọc và ticket chưa kết luận trước khi trả phòng. */
export async function getMoveOutPreview(
  contractId: number,
  moveOutDate: string,
): Promise<MoveOutPreview> {
  return (
    await api.get<MoveOutPreview>(`/contracts/${contractId}/move-out/preview`, {
      params: { moveOutDate },
    })
  ).data
}

/** Thực hiện trả phòng; backend kiểm tra lại cảnh báo đã xác nhận và trạng thái hợp đồng. */
export async function executeMoveOut(
  contractId: number,
  input: MoveOutRequest,
): Promise<MoveOutResult> {
  return (
    await api.post<MoveOutResult>(`/contracts/${contractId}/move-out`, input)
  ).data
}

/** Danh sách phiếu gửi đồ trong phạm vi quản lý; phân trang tại máy chủ. */
export async function searchStorageRecords(
  input: StorageSearchRequest,
): Promise<CheckoutPage<StorageRecordItem>> {
  return (
    await api.post<CheckoutPage<StorageRecordItem>>(
      '/storage-records/search-by-page',
      input,
    )
  ).data
}

/** Tạo phiếu gửi đồ cho hợp đồng đủ điều kiện tạm rời hè. */
export async function createStorageRecord(
  contractId: number,
  input: CreateStorageRequest,
): Promise<StorageRecordItem> {
  return (
    await api.post<StorageRecordItem>(
      `/contracts/${contractId}/storage-records`,
      input,
    )
  ).data
}

/** Phân lại giường và ghi ngày nhận lại đồ. */
export async function returnStorageRecord(
  storageRecordId: number,
  input: ReturnStorageRequest,
): Promise<StorageRecordItem> {
  return (
    await api.post<StorageRecordItem>(
      `/storage-records/${storageRecordId}/return`,
      input,
    )
  ).data
}
