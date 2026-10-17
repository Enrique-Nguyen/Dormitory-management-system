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
