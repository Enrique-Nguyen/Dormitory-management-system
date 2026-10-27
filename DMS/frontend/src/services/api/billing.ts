import { ContractStatus, DepositType, InvoiceType, InvoiceStatus, PaymentMethod } from '../../constants/statusConfig'
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
  itemType: string
  quantity: number
  unitPrice: number
  amount: number
  note: string | null
}

export interface InvoiceDetail {
  id: number
  code: string
  type: (typeof InvoiceType)[keyof typeof InvoiceType]
  studentId: number | null
  semesterId: number | null
  roomId: number | null
  billingMonth: string | null
  totalAmount: number
  dueDate: string
  status: (typeof InvoiceStatus)[keyof typeof InvoiceStatus]
  paidAt: string | null
  items: InvoiceItem[] | null
  allowedActions: string[] | null
}

export interface IssueRoomFeeRequest {
  semesterId: number
}

export interface IssueUtilityRequest {
  managementUnitId?: number
  billingMonth: string // YYYY-MM-DD
}

export interface ConfirmPaymentRequest {
  paymentMethod: (typeof PaymentMethod)[keyof typeof PaymentMethod]
  payerUserId?: number
  payerName?: string
}

export interface DepositTransaction {
  id: number
  contractId: number
  type: (typeof DepositType)[keyof typeof DepositType]
  amount: number
  maintenanceRequestId: number | null
  dueDate: string | null
  confirmedBy: number | null
  createdAt: string
  note: string | null
}

export interface DepositBalance {
  contractId: number
  studentId: number
  contractCode: string
  studentName: string
  studentCode: string | null
  status: (typeof ContractStatus)[keyof typeof ContractStatus]
  depositRequired: number | null
  balance: number
  topupNeeded: number
  oldestUnpaidDue: string | null
  topupOverdue: boolean
  depositDebt: number
  allowedActions: string[]
}

export interface BatchIssueResult {
  totalCreated: number
  totalSkipped: number
  totalAmount: number
}

export interface StaffDashboardSummary {
  managementUnitId: number | null
  unitName: string | null
  capacity: {
    operationalBeds: number
    occupiedBeds: number
    availableBeds: number
    maintenanceBeds: number
    occupancyRate: number
  }
  finance: {
    totalUnpaidAmount: number
    unpaidRoomFeeAmount: number
    unpaidUtilityAmount: number
    totalTopupNeededAmount: number
  }
  operations: {
    roomsMissingLeader: number
    contractsExpiringSoon: number
    contractsOverdueMoveout: number
    ticketsByStatus: Record<string, number>
    ticketsRequiringAction?: number
  }
  infrastructure?: {
    buildings: number
    rooms: number
    roomsByStatus: Record<string, number>
  }
  registration?: {
    totalApplications: number
    pendingApplications: number
    pendingSummerChoices: number
    applicationsByStatus: Record<string, number>
  }
  residency?: {
    activeContracts: number
    pendingActivation: number
    pendingExtensions: number
    pendingTransfers: number
    storageRecords: number
    storedPackages: number
    contractsByStatus: Record<string, number>
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
): Promise<BatchIssueResult> {
  return (await api.post<BatchIssueResult>('/invoices/issue-room-fee', data)).data
}

export async function issueUtilityInvoices(
  data: IssueUtilityRequest,
): Promise<BatchIssueResult> {
  return (await api.post<BatchIssueResult>('/invoices/issue-utility', data)).data
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
