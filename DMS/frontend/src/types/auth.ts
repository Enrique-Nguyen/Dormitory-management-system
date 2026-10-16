import type { Role } from '../constants/authorization'

// Schema của GET /api/auth/me (API Spec mục 4.1).
export type { Role } from '../constants/authorization'

export interface UnitRef {
  id: number
  code: string
  name: string
}

export interface StudentInfo {
  studentCode: string
  gender: 'M' | 'F'
  isRoomLeader: boolean
  hasActiveContract: boolean
}

export interface Me {
  id: number
  username: string
  fullName: string
  role: Role
  /** Quyền thao tác do backend trả về; giao diện không tự suy quyền từ tên role. */
  permissions: string[]
  managementUnit: UnitRef | null
  crossUnitAccess: boolean
  mustChangePassword: boolean
  student: StudentInfo | null
}
