import { api } from './client'

// API Spec 3.4, 4.7: chi tiết và xét duyệt yêu cầu chuyển phòng.

export interface TimelineEntry {
  fromStatus: string | null
  toStatus: string
  actorId: number | null
  reason: string | null
  at: string
}

export interface CurrentBed {
  assignmentId: number
  studentId: number
  contractId: number
  kind: 'HOLD' | 'STAY'
  bedId: number
  bedCode: string
  roomId: number
  roomCode: string
  buildingId: number
  buildingCode: string
  managementUnitId: number
}

export interface TransferDetail {
  id: number
  contractId: number
  contractCode: string
  studentId: number
  studentCode: string
  studentName: string
  reason: string
  status: string
  targetBedId: number | null
  decidedBy: number | null
  decidedAt: string | null
  decisionReason: string | null
  createdAt: string
  currentBed: CurrentBed | null
  allowedActions: string[]
  timeline: TimelineEntry[]
}

const base = '/transfer-requests'

export async function getTransferRequest(id: number): Promise<TransferDetail> {
  return (await api.get<TransferDetail>(`${base}/${id}`)).data
}

export async function approveTransfer(
  id: number,
  targetBedId: number,
): Promise<TransferDetail> {
  return (
    await api.post<TransferDetail>(`${base}/${id}/approve`, { targetBedId })
  ).data
}

export async function rejectTransfer(
  id: number,
  reason: string,
): Promise<TransferDetail> {
  return (await api.post<TransferDetail>(`${base}/${id}/reject`, { reason }))
    .data
}

export async function cancelTransfer(id: number): Promise<TransferDetail> {
  return (await api.post<TransferDetail>(`${base}/${id}/cancel`)).data
}

// ---------------------------------------------------------------------------------------------
// TV3: chi tiết hợp đồng, kích hoạt, yêu cầu gia hạn và hộp yêu cầu chờ xử lý.
// Response chi tiết được đọc tolerant: backend có thể chưa trả đủ student/deposit/labels.
// ---------------------------------------------------------------------------------------------

/** Trang dữ liệu dùng chung cho các endpoint POST search-by-page. */
export interface PageResponse<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

/** Mã hành động trong `allowedActions` của hợp đồng (API Spec mục 6). */
export const ContractAction = {
  ACTIVATE: 'activate',
  CREATE_EXTENSION_REQUEST: 'create-extension-request',
  CREATE_TRANSFER_REQUEST: 'create-transfer-request',
  MOVE_OUT: 'move-out',
  CREATE_STORAGE_RECORD: 'create-storage-record',
  /** Backend dùng chung mã hành động cọc `deposit-topup` (nhánh TOP_UP giữ cho các gọi không đổi). */
  TOP_UP: 'deposit-topup',
} as const

/** Mã hành động của yêu cầu gia hạn. */
export const ExtensionAction = {
  APPROVE: 'approve',
  REJECT: 'reject',
  CANCEL: 'cancel',
} as const

/** Trạng thái yêu cầu gia hạn (khớp `contract_extension_request.status`). */
export const ExtensionStatus = {
  PENDING: 'PENDING',
  APPROVED: 'APPROVED',
  REJECTED: 'REJECTED',
  CANCELLED: 'CANCELLED',
} as const

/** Mã loại việc trong hộp yêu cầu chờ xử lý. */
export const PendingItemType = {
  APPLICATION: 'APPLICATION',
  EXTENSION: 'EXTENSION',
  TRANSFER: 'TRANSFER',
  /** Wire value của backend là SUMMER_CHOICE; nhãn hiển thị vẫn là "Lựa chọn hè". */
  SUMMER: 'SUMMER_CHOICE',
  CONTRACT: 'CONTRACT',
  TOP_UP: 'TOP_UP',
} as const

export interface ContractStudent {
  id?: number | null
  studentCode?: string | null
  fullName?: string | null
  gender?: string | null
}

export interface ContractBed {
  id?: number | null
  code?: string | null
  bedCode?: string | null
  roomId?: number | null
  roomCode?: string | null
  buildingId?: number | null
  buildingCode?: string | null
  floor?: number | null
  unitId?: number | null
}

/** Một dòng lịch sử phân phòng; có thể lồng `bed` hoặc trả phẳng các mã. */
export interface ContractAssignment {
  id?: number | null
  kind?: string | null
  bed?: ContractBed | null
  bedId?: number | null
  bedCode?: string | null
  roomId?: number | null
  roomCode?: string | null
  buildingId?: number | null
  buildingCode?: string | null
  floor?: number | null
  startedAt?: string | null
  stayStartedAt?: string | null
  endedAt?: string | null
  endReason?: string | null
  priceMain?: number | null
  priceSummer?: number | null
}

export interface ContractDeposit {
  required?: number | null
  balance?: number | null
  topUpRequired?: number | null
  topUpOverdue?: boolean | null
}

/** Ba yêu cầu đang chờ của hợp đồng (API Spec 4.5). */
export interface ContractPendingRequests {
  extensionRequestId?: number | null
  transferRequestId?: number | null
  summerChoiceId?: number | null
}

/** Dạng danh sách phòng khi backend trả mảng yêu cầu chờ thay vì object. */
export interface ContractPendingRequestEntry {
  type?: string | null
  id?: number | null
  entityId?: number | null
  status?: string | null
}

export interface ContractParticipant {
  id?: number | null
  fullName?: string | null
  username?: string | null
}

export interface ContractMoveOut {
  id?: number | null
  moveOutDate?: string | null
  assetCondition?: string | null
  createdAt?: string | null
}

export interface ContractDetail {
  id: number
  code?: string | null
  status: string
  studentId?: number | null
  student?: ContractStudent | null
  /** Tên phẳng của backend khung cũ; đọc được cả object lẫn các trường rời. */
  studentName?: string | null
  studentCode?: string | null
  fullName?: string | null
  startDate?: string | null
  endDate?: string | null
  activationDeadline?: string | null
  activatedAt?: string | null
  activatedBy?: number | ContractParticipant | null
  deposit?: ContractDeposit | null
  /** Tên cũ của backend khung; giữ để đọc được cả hai. */
  depositRequired?: number | null
  depositBalance?: number | null
  topUpRequired?: number | null
  topUpOverdue?: boolean | null
  /** EXPIRING, OVERDUE_MOVE_OUT, SUMMER_LEAVE do backend tính (BR-08). */
  labels?: string[] | null
  daysRemaining?: number | null
  currentAssignment?: ContractAssignment | null
  assignments?: ContractAssignment[] | null
  assignmentHistory?: ContractAssignment[] | null
  history?: ContractAssignment[] | null
  pendingRequests?: ContractPendingRequests | ContractPendingRequestEntry[] | null
  /** Backend trả phẳng ba ID yêu cầu đang chờ; vẫn đọc thêm dạng object/mảng ở trên. */
  pendingExtensionRequestId?: number | null
  pendingTransferRequestId?: number | null
  pendingSummerChoiceId?: number | null
  openStorageRecordId?: number | null
  moveOut?: ContractMoveOut | null
  statusReason?: string | null
  allowedActions?: string[] | null
  timeline?: TimelineEntry[] | null
}

export interface ActivateContractRequest {
  paperContractSigned: boolean
  depositAmount: number
  /** CASH hoặc TRANSFER (PaymentMethod ở constants/statusConfig). */
  paymentMethod: string
  note?: string
}

export interface ActivationDepositSummary {
  type?: string | null
  amount?: number | null
  balance?: number | null
  paymentMethod?: string | null
  collectedAt?: string | null
  note?: string | null
}

/** Kết quả kích hoạt: hợp đồng đã ACTIVE, khoản cọc vừa thu và phân phòng hiện tại. */
export interface ActivationResult {
  contract?: ContractDetail | null
  deposit?: ActivationDepositSummary | null
  currentAssignment?: ContractAssignment | null
  allowedActions?: string[] | null
}

export interface ExtensionOverlap {
  contractId?: number | null
  contractCode?: string | null
  studentId?: number | null
  studentName?: string | null
  startDate?: string | null
  endDate?: string | null
  status?: string | null
}

export interface ExtensionDetail {
  id: number
  contractId?: number | null
  contractCode?: string | null
  studentId?: number | null
  student?: ContractStudent | null
  studentName?: string | null
  fullName?: string | null
  studentCode?: string | null
  reason?: string | null
  status: string
  newEndDate?: string | null
  /** Ngày kết thúc hiện tại; backend có thể dùng một trong các tên dưới. */
  oldEndDate?: string | null
  currentEndDate?: string | null
  contractEndDate?: string | null
  decidedBy?: number | null
  decidedByName?: string | null
  decidedAt?: string | null
  decisionReason?: string | null
  submittedAt?: string | null
  createdAt?: string | null
  /** Hợp đồng bị chồng khoảng lưu trú (nếu backend kiểm tra trước). */
  overlap?: ExtensionOverlap | ExtensionOverlap[] | null
  overlaps?: ExtensionOverlap[] | null
  overlappingContract?: ExtensionOverlap | null
  overlapContract?: ExtensionOverlap | null
  allowedActions?: string[] | null
  timeline?: TimelineEntry[] | null
}

export interface SaveExtensionRequest {
  id?: number
  contractId: number
  newEndDate: string
  reason: string
}

/** Một việc trong hộp yêu cầu chờ xử lý (API Spec 4.15). */
export interface PendingItem {
  id?: number | null
  type?: string | null
  entityId?: number | null
  student?: ContractStudent | string | null
  studentId?: number | null
  studentName?: string | null
  studentCode?: string | null
  submittedAt?: string | null
  status?: string | null
  label?: string | null
}

export interface PendingItemsRequest {
  page: number
  size: number
  q?: string
  type?: string
}

export type ContractPendingRequestType = 'extension' | 'transfer' | 'summer'

export interface ContractPendingRef {
  type: ContractPendingRequestType
  id: number
}

const contractBase = '/contracts'
const extensionBase = '/extension-requests'
const pendingItemBase = '/pending-items'

export async function getContract(id: number): Promise<ContractDetail> {
  return (await api.get<ContractDetail>(`${contractBase}/${id}`)).data
}

export async function activateContract(
  id: number,
  input: ActivateContractRequest,
): Promise<ActivationResult> {
  return (
    await api.post<ActivationResult>(`${contractBase}/${id}/activate`, input)
  ).data
}

export async function getExtensionRequest(
  id: number,
): Promise<ExtensionDetail> {
  return (await api.get<ExtensionDetail>(`${extensionBase}/${id}`)).data
}

/** Tạo yêu cầu gia hạn; hợp đồng nguồn chỉ gửi khi tạo, không đổi khi sửa. */
export async function createExtensionRequest(
  input: SaveExtensionRequest,
): Promise<ExtensionDetail> {
  return (
    await api.post<ExtensionDetail>(`${extensionBase}/save-or-update`, input)
  ).data
}

export async function approveExtension(
  id: number,
): Promise<ExtensionDetail> {
  return (
    await api.post<ExtensionDetail>(`${extensionBase}/${id}/approve`, {})
  ).data
}

export async function rejectExtension(
  id: number,
  reason: string,
): Promise<ExtensionDetail> {
  return (
    await api.post<ExtensionDetail>(`${extensionBase}/${id}/reject`, { reason })
  ).data
}

export async function cancelExtension(
  id: number,
): Promise<ExtensionDetail> {
  return (await api.post<ExtensionDetail>(`${extensionBase}/${id}/cancel`))
    .data
}

export async function searchPendingItems(
  request: PendingItemsRequest,
): Promise<PageResponse<PendingItem>> {
  const data: unknown = (
    await api.post(`${pendingItemBase}/search-by-page`, request)
  ).data
  return normalizePage<PendingItem>(data)
}

function normalizePage<T>(data: unknown): PageResponse<T> {
  if (Array.isArray(data)) {
    return {
      content: data as T[],
      page: 0,
      size: data.length,
      totalElements: data.length,
      totalPages: 1,
    }
  }
  if (data && typeof data === 'object') {
    const value = data as Record<string, unknown>
    if (Array.isArray(value.content)) {
      const content = value.content as T[]
      return {
        content,
        page: numberOr(value.page, 0),
        size: numberOr(value.size, content.length),
        totalElements: numberOr(value.totalElements, content.length),
        totalPages: numberOr(value.totalPages, 1),
      }
    }
  }
  return { content: [], page: 0, size: 0, totalElements: 0, totalPages: 0 }
}

function numberOr(value: unknown, fallback: number): number {
  const parsed = Number(value)
  return Number.isFinite(parsed) ? parsed : fallback
}

export function contractCodeOf(detail: ContractDetail): string {
  return detail.code?.trim() || `#${detail.id}`
}

export function contractStudentInfo(detail: ContractDetail) {
  return {
    fullName:
      detail.student?.fullName?.trim() ||
      detail.studentName?.trim() ||
      detail.fullName?.trim() ||
      null,
    studentCode:
      detail.student?.studentCode?.trim() ||
      detail.studentCode?.trim() ||
      null,
    gender: detail.student?.gender ?? null,
    studentId: detail.student?.id ?? detail.studentId ?? null,
  }
}

export function contractDepositInfo(detail: ContractDetail) {
  const deposit = detail.deposit ?? null
  return {
    required: deposit?.required ?? detail.depositRequired ?? null,
    balance: deposit?.balance ?? detail.depositBalance ?? null,
    topUpRequired:
      deposit?.topUpRequired ?? detail.topUpRequired ?? null,
    topUpOverdue:
      deposit?.topUpOverdue === true || detail.topUpOverdue === true,
  }
}

export function contractAssignments(
  detail: ContractDetail,
): ContractAssignment[] {
  return (
    detail.assignments ?? detail.assignmentHistory ?? detail.history ?? []
  )
}

/** Ghép mã giường đầy đủ từ object lồng hoặc các trường phẳng backend trả. */
export function contractBedLabel(assignment: ContractAssignment): string {
  const bed = assignment.bed
  if (bed?.code) return bed.code
  const building = bed?.buildingCode ?? assignment.buildingCode
  const room = bed?.roomCode ?? assignment.roomCode
  const bedCode = bed?.bedCode ?? assignment.bedCode
  if (building && room && bedCode) return `${building}-${room}-${bedCode}`
  const id = bed?.id ?? assignment.bedId
  return id != null ? `Giường #${id}` : '—'
}

export function contractActorLabel(
  actor: ContractDetail['activatedBy'],
): string {
  if (actor == null) return '—'
  if (typeof actor === 'number') return `Người dùng #${actor}`
  const name = actor.fullName?.trim() || actor.username?.trim()
  if (name) return name
  return actor.id != null ? `Người dùng #${actor.id}` : '—'
}

export function extensionStudentInfo(detail: ExtensionDetail) {
  return {
    fullName:
      detail.student?.fullName?.trim() ||
      detail.studentName?.trim() ||
      detail.fullName?.trim() ||
      null,
    studentCode:
      detail.student?.studentCode?.trim() ||
      detail.studentCode?.trim() ||
      null,
    studentId: detail.student?.id ?? detail.studentId ?? null,
  }
}

export function extensionOldEndDate(detail: ExtensionDetail): string | null {
  return (
    detail.oldEndDate ??
    detail.currentEndDate ??
    detail.contractEndDate ??
    null
  )
}

export function extensionOverlapList(
  detail: ExtensionDetail,
): ExtensionOverlap[] {
  const value =
    detail.overlaps ??
    detail.overlap ??
    detail.overlappingContract ??
    detail.overlapContract
  if (!value) return []
  const list = Array.isArray(value) ? value : [value]
  return list.filter((item) => item != null)
}

function normalizePendingRefType(
  type: string | null | undefined,
): ContractPendingRequestType | null {
  const value = (type ?? '').toUpperCase()
  if (value.includes('EXTENSION')) return 'extension'
  if (value.includes('TRANSFER')) return 'transfer'
  if (value.includes('SUMMER')) return 'summer'
  return null
}

/** Danh sách yêu cầu đang chờ, đọc được object `pendingRequests` lẫn mảng chi tiết. */
export function contractPendingRefs(
  detail: ContractDetail,
): ContractPendingRef[] {
  const pending = detail.pendingRequests
  const refs: ContractPendingRef[] = []
  if (Array.isArray(pending)) {
    for (const entry of pending) {
      const id = entry.entityId ?? entry.id
      const type = normalizePendingRefType(entry.type)
      if (id != null && type) refs.push({ type, id })
    }
    return refs
  }
  if (pending) {
    const objectPending = pending as ContractPendingRequests
    if (objectPending.extensionRequestId != null)
      refs.push({ type: 'extension', id: objectPending.extensionRequestId })
    if (objectPending.transferRequestId != null)
      refs.push({ type: 'transfer', id: objectPending.transferRequestId })
    if (objectPending.summerChoiceId != null)
      refs.push({ type: 'summer', id: objectPending.summerChoiceId })
  }
  if (detail.pendingExtensionRequestId != null)
    refs.push({ type: 'extension', id: detail.pendingExtensionRequestId })
  if (detail.pendingTransferRequestId != null)
    refs.push({ type: 'transfer', id: detail.pendingTransferRequestId })
  if (detail.pendingSummerChoiceId != null)
    refs.push({ type: 'summer', id: detail.pendingSummerChoiceId })
  const seen = new Set<string>()
  return refs.filter((ref) => {
    const key = `${ref.type}-${ref.id}`
    if (seen.has(key)) return false
    seen.add(key)
    return true
  })
}

export function pendingItemTypeKey(type: string | null | undefined): string {
  const value = (type ?? '').toUpperCase()
  if (value.includes('EXTENSION')) return PendingItemType.EXTENSION
  if (value.includes('SUMMER')) return PendingItemType.SUMMER
  if (value.includes('TRANSFER')) return PendingItemType.TRANSFER
  if (value.includes('CONTRACT') && !value.includes('EXTENSION'))
    return PendingItemType.CONTRACT
  if (value.includes('TOP')) return PendingItemType.TOP_UP
  if (value.includes('APPLICATION')) return PendingItemType.APPLICATION
  return value || 'UNKNOWN'
}

export function pendingItemStudent(detail: PendingItem) {
  const student = detail.student
  if (typeof student === 'string') return { fullName: student, studentCode: null }
  return {
    fullName:
      student?.fullName?.trim() || detail.studentName?.trim() || null,
    studentCode:
      student?.studentCode?.trim() || detail.studentCode?.trim() || null,
  }
}

export function pendingItemEntityId(detail: PendingItem): number | null {
  return detail.entityId ?? detail.id ?? null
}
