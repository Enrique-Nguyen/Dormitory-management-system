import { api } from './client'

// API Spec 4.3–4.4: chi tiết phòng, trưởng phòng, giường trống cho split view.

export interface StudentRef {
  id: number
  fullName: string
  studentCode: string
}

export interface RoomOccupant {
  studentId: number
  fullName: string
  studentCode: string
  /** HOLD = đang giữ chỗ, STAY = đang ở. */
  kind: 'HOLD' | 'STAY'
}

export interface RoomBedItem {
  id: number
  code: string
  status: string
  occupant: RoomOccupant | null
}

export interface LeaderChange {
  from: StudentRef | null
  to: StudentRef | null
  /** null = hệ thống tự gỡ khi phân phòng đóng. */
  actorId: number | null
  actorName: string | null
  reason: string | null
  at: string
}

export interface RoomDetail {
  id: number
  code: string
  floor: number
  gender: string
  status: string
  buildingId: number
  roomTypeId: number
  leaderStudentId: number | null
  activeBeds: number
  heldBeds: number
  occupiedBeds: number
  freeBeds: number
  missingLeader: boolean
  leader: StudentRef | null
  beds: RoomBedItem[]
  leaderHistory: LeaderChange[]
}

export interface AvailableRoom {
  room: {
    id: number
    code: string
    buildingId: number
    buildingCode: string
    floor: number
    roomType: { id: number; name: string }
    gender: string
    priceMain: number
  }
  matchesPreferredType: boolean
  freeBeds: { id: number; code: string }[]
  activeBeds: number
  occupiedCount: number
  heldCount: number
}

/** Đúng một nguồn: đơn đã duyệt (TV3) hoặc yêu cầu chuyển phòng. */
export interface AvailableBedsParams {
  applicationId?: number
  transferRequestId?: number
  storageRecordId?: number
  buildingId?: number
  roomTypeId?: number
}

export async function getRoom(id: number): Promise<RoomDetail> {
  return (await api.get<RoomDetail>(`/rooms/${id}`)).data
}

export async function assignRoomLeader(
  id: number,
  studentId: number,
): Promise<RoomDetail> {
  return (await api.put<RoomDetail>(`/rooms/${id}/leader`, { studentId })).data
}

export async function removeRoomLeader(id: number, reason: string) {
  await api.delete(`/rooms/${id}/leader`, { data: { reason } })
}

export async function getAvailableBeds(
  params: AvailableBedsParams,
): Promise<AvailableRoom[]> {
  return (await api.get<AvailableRoom[]>('/beds/available', { params })).data
}
