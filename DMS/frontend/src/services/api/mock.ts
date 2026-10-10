import type { Me } from '../../types/auth'

// Chỉ dùng khi phát triển lúc backend chưa sẵn sàng. Đặt VITE_MOCK_ROLE trong .env.local (xem .env.example).
// Bản build production luôn gọi API thật.
export type MockRole = 'student' | 'student-must-change' | 'student-no-contract' | 'staff' | 'staff-cross' | 'technician' | 'admin'

// Đổi vai trò nhanh khi dev: mở bất kỳ trang nào với ?mockRole=staff (lưu lại cho các lần sau; ?mockRole=off để xóa).
function resolveMockRole(): MockRole | null {
  if (!import.meta.env.DEV) return null
  const fromEnv = import.meta.env.VITE_MOCK_ROLE as MockRole | undefined
  if (!fromEnv) return null
  try {
    const param = new URLSearchParams(window.location.search).get('mockRole')
    if (param === 'off') localStorage.removeItem('dms.mockRole')
    else if (param) localStorage.setItem('dms.mockRole', param)
    return (localStorage.getItem('dms.mockRole') as MockRole | null) ?? fromEnv
  } catch {
    return fromEnv
  }
}

export const mockRole: MockRole | null = resolveMockRole()

const studentBase: Me = {
  id: 501,
  username: '2251060001',
  fullName: 'Nguyễn Văn A',
  role: 'STUDENT',
  managementUnit: null,
  crossUnitAccess: false,
  mustChangePassword: false,
  student: { studentCode: '2251060001', gender: 'M', isRoomLeader: false, hasActiveContract: true },
}

const staffBase: Me = {
  id: 201,
  username: 'staff.k1',
  fullName: 'Trần Thị B',
  role: 'STAFF',
  managementUnit: { id: 1, code: 'K1', name: 'Ký túc xá K1' },
  crossUnitAccess: false,
  mustChangePassword: false,
  student: null,
}

const mockUsers: Record<MockRole, Me> = {
  student: studentBase,
  'student-must-change': { ...studentBase, mustChangePassword: true },
  'student-no-contract': { ...studentBase, student: { ...studentBase.student!, hasActiveContract: false } },
  staff: staffBase,
  'staff-cross': { ...staffBase, crossUnitAccess: true },
  technician: { ...staffBase, id: 301, username: 'ktv.k1', fullName: 'Lê Văn C', role: 'TECHNICIAN' },
  admin: { ...staffBase, id: 1, username: 'admin', fullName: 'Quản trị viên', role: 'ADMIN', managementUnit: null },
}

let passwordChanged = false

/** Mô phỏng đổi mật khẩu thành công: bỏ cờ mustChangePassword cho tới khi tải lại trang. */
export function markMockPasswordChanged() {
  passwordChanged = true
}

export function getMockMe(): Me {
  const me = mockUsers[mockRole ?? 'student'] ?? studentBase
  return passwordChanged ? { ...me, mustChangePassword: false } : me
}
