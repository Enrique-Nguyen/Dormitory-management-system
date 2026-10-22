import { api } from './client'

// API Spec: Phân hệ 6 (Điện nước, Hóa đơn & Tiền cọc) & Phân hệ 7 (Dashboard & Báo cáo) - TV5

export interface PageResponse<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export interface InvoiceItem {
  id: number
  description: string
  quantity: number
  unitPrice: number
  amount: number
  kind: string
}

export interface InvoiceDetail {
  id: number
  code: string
  type: 'ROOM_FEE' | 'UTILITY'
  studentId?: number
  studentCode?: string
  studentName?: string
  semesterId?: number
  semesterName?: string
  roomId?: number
  roomCode?: string
  buildingCode?: string
  billingMonth?: string
  totalAmount: number
  issuedAt: string
  dueDate: string
  status: 'UNPAID' | 'PAID'
  paidAt?: string
  paymentMethod?: 'CASH' | 'TRANSFER'
  payerName?: string
  confirmedBy?: number
  items?: InvoiceItem[]
}

export interface IssueRoomFeeRequest {
  semesterId: number
}

export interface IssueUtilityRequest {
  managementUnitId: number
  billingMonth: string // YYYY-MM-DD
}

export interface ConfirmPaymentRequest {
  paymentMethod: 'CASH' | 'TRANSFER'
  payerUserId?: number
  payerName?: string
}

export interface DepositTransaction {
  id: number
  contractId: number
  studentId: number
  studentCode?: string
  studentName?: string
  type: 'DEPOSIT' | 'DEDUCTION' | 'TOPUP' | 'REFUND'
  amount: number
  balanceAfter: number
  refTicketId?: number
  refInvoiceId?: number
  note?: string
  createdAt: string
  staffName?: string
}

export interface DepositBalance {
  contractId: number
  contractCode?: string
  studentId: number
  studentCode?: string
  studentName?: string
  roomCode?: string
  buildingCode?: string
  depositRequired: number
  balance: number
  status: 'NORMAL' | 'TOPUP_NEEDED' | 'REFUNDED'
}

export interface StudentDebt {
  studentId: number
  unpaidRoomFee: number
  unpaidUtilityShare: number
  depositTopupNeeded: number
  totalDebt: number
  hasDebt: boolean
}

export interface RoomDebt {
  roomId: number
  unpaidUtilityAmount: number
  unpaidMonthsCount: number
  hasDebt: boolean
}

export interface StaffDashboardSummary {
  managementUnitId: number
  managementUnitName: string
  capacity: {
    totalBeds: number
    occupiedBeds: number
    heldBeds: number
    freeBeds: number
    occupancyRate: number
  }
  financial: {
    unpaidInvoicesCount: number
    unpaidInvoicesAmount: number
    topupNeededCount: number
  }
  operations: {
    roomsMissingLeader: number
    expiringContractsCount: number
    pendingTicketsCount: number
    openTicketsByCategory: Record<string, number>
  }
}

// ---------------------------------------------
// API Calls
// ---------------------------------------------

export async function getStaffDashboard(): Promise<StaffDashboardSummary> {
  return (await api.get<StaffDashboardSummary>('/staff/dashboard')).data
}

export async function issueRoomFeeInvoices(
  data: IssueRoomFeeRequest,
): Promise<{ count: number }> {
  return (await api.post<{ count: number }>('/invoices/issue-room-fee', data)).data
}

export async function issueUtilityInvoices(
  data: IssueUtilityRequest,
): Promise<{ count: number }> {
  return (await api.post<{ count: number }>('/invoices/issue-utility', data)).data
}

export async function confirmInvoicePayment(
  invoiceId: number,
  data: ConfirmPaymentRequest,
): Promise<void> {
  await api.post(`/invoices/${invoiceId}/confirm-payment`, data)
}

export async function getDepositBalance(
  contractId: number,
): Promise<DepositBalance> {
  return (await api.get<DepositBalance>(`/contracts/${contractId}/deposit-balance`)).data
}

export async function listDepositTransactions(
  contractId: number,
): Promise<DepositTransaction[]> {
  return (
    await api.get<DepositTransaction[]>(`/contracts/${contractId}/deposit-transactions`)
  ).data
}

export async function recordDepositTopup(data: {
  contractId: number
  amount: number
  note?: string
}): Promise<DepositTransaction> {
  return (
    await api.post<DepositTransaction>(
      `/contracts/${data.contractId}/deposit-topup`,
      { amount: data.amount, note: data.note },
    )
  ).data
}

export async function recordDepositRefund(data: {
  contractId: number
  amount: number
  note?: string
}): Promise<DepositTransaction> {
  return (
    await api.post<DepositTransaction>(
      `/contracts/${data.contractId}/deposit-refund`,
      { amount: data.amount, note: data.note },
    )
  ).data
}

export async function getStudentDebt(studentId: number): Promise<StudentDebt> {
  return (await api.get<StudentDebt>(`/billing/debt/student/${studentId}`)).data
}

export async function getRoomDebt(roomId: number): Promise<RoomDebt> {
  return (await api.get<RoomDebt>(`/billing/debt/room/${roomId}`)).data
}
