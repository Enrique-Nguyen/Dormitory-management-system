import { api } from './client'
import type { ContractStudent, TimelineEntry } from './contract'

// API Spec 4.13–4.15: chi tiết, duyệt/từ chối lựa chọn kỳ hè và lựa chọn hiện tại của sinh viên.

/** Mã hành động trong `allowedActions` của lựa chọn hè. */
export const SummerChoiceAction = {
  APPROVE: 'approve',
  REJECT: 'reject',
  EDIT: 'edit',
} as const

/** Trạng thái lựa chọn hè (khớp CHECK `summer_choice.status`: chỉ ba giá trị này). */
export const SummerChoiceStatus = {
  PENDING: 'PENDING',
  APPROVED: 'APPROVED',
  REJECTED: 'REJECTED',
} as const

/** Phương án hè (khớp `summer_choice.choice`). */
export const SummerChoice = {
  STAY: 'STAY',
  TEMP_LEAVE: 'TEMP_LEAVE',
  LEAVE: 'LEAVE',
} as const

export interface SummerSemester {
  id?: number | null
  name?: string | null
  academicYear?: string | null
  type?: string | null
  startDate?: string | null
  endDate?: string | null
}

/** Hợp đồng nguồn của lựa chọn hè; backend có thể chỉ trả contractCode. */
export interface SummerContract {
  id?: number | null
  code?: string | null
  status?: string | null
  startDate?: string | null
  endDate?: string | null
  student?: ContractStudent | null
}

export interface SummerChoiceDetail {
  id: number
  contractId?: number | null
  contractCode?: string | null
  contract?: SummerContract | null
  /** Backend chi tiết trả phẳng trạng thái/ngày kết thúc của hợp đồng nguồn. */
  contractStatus?: string | null
  contractEndDate?: string | null
  studentId?: number | null
  student?: ContractStudent | null
  fullName?: string | null
  studentName?: string | null
  studentCode?: string | null
  semesterId?: number | null
  semesterName?: string | null
  semester?: SummerSemester | null
  choice: string
  status: string
  /** `summer_choice` không có cột lý do; ngày gửi là `createdAt`. */
  createdAt?: string | null
  decidedBy?: number | null
  decidedAt?: string | null
  decisionReason?: string | null
  allowedActions?: string[] | null
  timeline?: TimelineEntry[] | null
}

const base = '/summer-choices'

export async function getSummerChoice(id: number): Promise<SummerChoiceDetail> {
  return (await api.get<SummerChoiceDetail>(`${base}/${id}`)).data
}

export async function approveSummerChoice(
  id: number,
): Promise<SummerChoiceDetail> {
  return (
    await api.post<SummerChoiceDetail>(`${base}/${id}/approve`, {})
  ).data
}

export async function rejectSummerChoice(
  id: number,
  reason: string,
): Promise<SummerChoiceDetail> {
  return (
    await api.post<SummerChoiceDetail>(`${base}/${id}/reject`, { reason })
  ).data
}

export function summerChoiceStudent(detail: SummerChoiceDetail) {
  return {
    fullName:
      detail.student?.fullName?.trim() ||
      detail.studentName?.trim() ||
      detail.fullName?.trim() ||
      detail.contract?.student?.fullName?.trim() ||
      null,
    studentCode:
      detail.student?.studentCode?.trim() ||
      detail.studentCode?.trim() ||
      detail.contract?.student?.studentCode?.trim() ||
      null,
    studentId:
      detail.student?.id ??
      detail.studentId ??
      detail.contract?.student?.id ??
      null,
  }
}

export function summerChoiceContractCode(
  detail: SummerChoiceDetail,
): string {
  return (
    detail.contractCode?.trim() ||
    detail.contract?.code?.trim() ||
    (detail.contractId != null ? `#${detail.contractId}` : '—')
  )
}

export function summerChoiceSemesterLabel(
  detail: SummerChoiceDetail,
): string {
  const semester = detail.semester
  const name = detail.semesterName?.trim() || semester?.name?.trim()
  if (name) {
    return semester?.academicYear
      ? `${name} · ${semester.academicYear}`
      : name
  }
  return detail.semesterId != null ? `Học kỳ #${detail.semesterId}` : '—'
}
