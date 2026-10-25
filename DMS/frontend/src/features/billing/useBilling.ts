// Feature billing (TV5): hook TanStack Query cho hóa đơn, sổ cọc, công nợ và staff dashboard.
// Hàm gọi API của module đặt ở services/api/billing.ts.
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  confirmInvoicePayment,
  getDepositBalance,
  getStaffDashboard,
  issueRoomFeeInvoices,
  issueUtilityInvoices,
  listDepositTransactions,
  recordDepositRefund,
  recordDepositTopup,
  type ConfirmPaymentRequest,
  type IssueRoomFeeRequest,
  type IssueUtilityRequest,
} from '../../services/api/billing'
import { useApiErrorHandler } from '../../hooks/useApiErrorHandler'


export const billingKeys = {
  dashboard: () => ['staff', 'dashboard'] as const,
  depositBalance: (contractId: number) =>
    ['deposits', 'balance', contractId] as const,
  depositTransactions: (contractId: number) =>
    ['deposits', 'transactions', contractId] as const,
}

/** Hook lấy dữ liệu tổng hợp cho Staff Dashboard (API 4.10) */
export function useStaffDashboard() {
  return useQuery({
    queryKey: billingKeys.dashboard(),
    queryFn: getStaffDashboard,
    staleTime: 60 * 1000,
  })
}

/** Hook lấy số dư cọc của hợp đồng */
export function useDepositBalance(contractId?: number) {
  return useQuery({
    queryKey: billingKeys.depositBalance(contractId ?? 0),
    queryFn: () => getDepositBalance(contractId!),
    enabled: Number.isSafeInteger(contractId) && (contractId ?? 0) > 0,
  })
}

/** Hook lấy lịch sử giao dịch cọc của hợp đồng */
export function useDepositTransactions(contractId?: number) {
  return useQuery({
    queryKey: billingKeys.depositTransactions(contractId ?? 0),
    queryFn: () => listDepositTransactions(contractId!),
    enabled: Number.isSafeInteger(contractId) && (contractId ?? 0) > 0,
  })
}

/** Hook phát hành hóa đơn tiền phòng hàng loạt */
export function useIssueRoomFeeInvoices() {
  const queryClient = useQueryClient()
  const onError = useApiErrorHandler()
  return useMutation({
    mutationFn: (data: IssueRoomFeeRequest) => issueRoomFeeInvoices(data),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['crud'] })
      void queryClient.invalidateQueries({ queryKey: billingKeys.dashboard() })
    },
    onError,
  })
}

/** Hook phát hành hóa đơn điện nước hàng loạt */
export function useIssueUtilityInvoices() {
  const queryClient = useQueryClient()
  const onError = useApiErrorHandler()
  return useMutation({
    mutationFn: (data: IssueUtilityRequest) => issueUtilityInvoices(data),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['crud'] })
      void queryClient.invalidateQueries({ queryKey: billingKeys.dashboard() })
    },
    onError,
  })
}

/** Hook xác nhận thanh toán hóa đơn */
export function useConfirmPayment(invoiceId: number) {
  const queryClient = useQueryClient()
  const onError = useApiErrorHandler()
  return useMutation({
    mutationFn: (data: ConfirmPaymentRequest) =>
      confirmInvoicePayment(invoiceId, data),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['crud'] })
      void queryClient.invalidateQueries({ queryKey: billingKeys.dashboard() })
    },
    onError,
  })
}

/** Hook nộp bù cọc */
export function useRecordDepositTopup(contractId: number) {
  const queryClient = useQueryClient()
  const onError = useApiErrorHandler()
  return useMutation({
    mutationFn: (data: { amount: number; note?: string }) =>
      recordDepositTopup({ contractId, ...data }),
    onSuccess: () => {
      void queryClient.invalidateQueries({
        queryKey: billingKeys.depositBalance(contractId),
      })
      void queryClient.invalidateQueries({
        queryKey: billingKeys.depositTransactions(contractId),
      })
      void queryClient.invalidateQueries({ queryKey: ['crud'] })
      void queryClient.invalidateQueries({ queryKey: billingKeys.dashboard() })
    },
    onError,
  })
}

/** Hook hoàn trả cọc */
export function useRecordDepositRefund(contractId: number) {
  const queryClient = useQueryClient()
  const onError = useApiErrorHandler()
  return useMutation({
    mutationFn: (data: { amount: number; note?: string }) =>
      recordDepositRefund({ contractId, ...data }),
    onSuccess: () => {
      void queryClient.invalidateQueries({
        queryKey: billingKeys.depositBalance(contractId),
      })
      void queryClient.invalidateQueries({
        queryKey: billingKeys.depositTransactions(contractId),
      })
      void queryClient.invalidateQueries({ queryKey: ['crud'] })
      void queryClient.invalidateQueries({ queryKey: billingKeys.dashboard() })
    },
    onError,
  })
}
