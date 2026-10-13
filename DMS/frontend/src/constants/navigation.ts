import {
  AuditOutlined,
  BankOutlined,
  BellOutlined,
  CalendarOutlined,
  ClockCircleOutlined,
  DashboardOutlined,
  DatabaseOutlined,
  FileTextOutlined,
  HomeOutlined,
  InboxOutlined,
  ProfileOutlined,
  ReconciliationOutlined,
  SafetyOutlined,
  SolutionOutlined,
  TeamOutlined,
  ThunderboltOutlined,
  ToolOutlined,
  UploadOutlined,
  UserOutlined,
  WalletOutlined,
} from '@ant-design/icons'
import type { ComponentType } from 'react'
import type { Role } from '../types/auth'

// Nguồn duy nhất cho menu và trang chủ từng vai trò (UI/UX mục 3, 4, 26).
export interface NavItem {
  key: string
  label: string
  path: string
  icon?: ComponentType
}

export interface NavGroup {
  title?: string
  items: NavItem[]
}

const item = (label: string, path: string, icon?: ComponentType): NavItem => ({
  key: path,
  label,
  path,
  icon,
})

/** Trang chủ của từng vai trò; route ngoài quyền chuyển về đây. */
export const roleHome: Record<Role, string> = {
  STUDENT: '/student',
  STAFF: '/staff/dashboard',
  TECHNICIAN: '/technician/tasks',
  ADMIN: '/admin/users',
}

export const roleLabel: Record<Role, string> = {
  STUDENT: 'Sinh viên',
  STAFF: 'Quản lý',
  TECHNICIAN: 'Kỹ thuật viên',
  ADMIN: 'Quản trị viên',
}

/** Mục "Yêu cầu chờ xử lý" mang badge số lượng (UI 4.2). */
export const STAFF_REQUESTS_PATH = '/staff/requests'

export const staffNav: NavGroup[] = [
  {
    items: [
      item('Dashboard', '/staff/dashboard', DashboardOutlined),
      item('Yêu cầu chờ xử lý', STAFF_REQUESTS_PATH, ClockCircleOutlined),
    ],
  },
  {
    title: 'Quản lý',
    items: [
      item('Đơn đăng ký', '/staff/applications', FileTextOutlined),
      item('Phòng & Giường', '/staff/rooms', HomeOutlined),
      item('Sinh viên', '/staff/students', TeamOutlined),
      item('Hợp đồng', '/staff/contracts', SolutionOutlined),
      item('Kỳ hè', '/staff/summer', InboxOutlined),
      item('Kho gửi đồ', '/staff/storage', InboxOutlined),
    ],
  },
  {
    title: 'Tài chính',
    items: [
      item('Điện nước', '/staff/utilities', ThunderboltOutlined),
      item('Đơn giá điện nước', '/staff/utilities/prices', ThunderboltOutlined),
      item('Hóa đơn', '/staff/invoices', ReconciliationOutlined),
      item('Tiền cọc', '/staff/deposits', WalletOutlined),
    ],
  },
  {
    title: 'Vận hành',
    items: [
      item('Sửa chữa', '/staff/maintenance', ToolOutlined),
      item('Thông báo', '/staff/notifications', BellOutlined),
    ],
  },
  {
    title: 'Hệ thống',
    items: [
      item('Loại phòng', '/staff/room-types', DatabaseOutlined),
      item('Kỳ học', '/staff/semesters', CalendarOutlined),
      item('Đợt đăng ký', '/staff/registration-periods', ProfileOutlined),
    ],
  },
]

export const adminNav: NavGroup[] = [
  {
    items: [
      item('Tài khoản', '/admin/users', UserOutlined),
      item('Sinh viên', '/admin/import', UploadOutlined),
      item('Đơn vị', '/admin/units', BankOutlined),
      item('Tòa nhà & mức cọc', '/admin/buildings', HomeOutlined),
      item('Quyền tổng hợp', '/admin/aggregate-permissions', SafetyOutlined),
      item('Diện ưu tiên', '/admin/priority-categories', DatabaseOutlined),
      item('Cấu hình', '/admin/configs', DatabaseOutlined),
      item('Nhật ký hệ thống', '/admin/audit-logs', AuditOutlined),
    ],
  },
]

/** Bottom nav sinh viên trên mobile (UI 4.1). Tab "Báo hỏng" chỉ bật khi đang ở. */
export const STUDENT_MAINTENANCE_PATH = '/student/maintenance'

export const studentBottomNav: NavItem[] = [
  item('Home', '/student', HomeOutlined),
  item('Phòng', '/student/room', HomeOutlined),
  item('Hóa đơn', '/student/invoices', ReconciliationOutlined),
  item('Báo hỏng', STUDENT_MAINTENANCE_PATH, ToolOutlined),
]

/** Mục còn lại của cổng sinh viên: nằm trong menu avatar trên mobile, menu ngang trên màn rộng. */
export const studentMoreNav: NavItem[] = [
  item('Đơn đăng ký', '/student/applications', FileTextOutlined),
  item('Hợp đồng', '/student/contracts', SolutionOutlined),
  item(
    'Yêu cầu gia hạn / chuyển phòng',
    '/student/requests',
    ClockCircleOutlined,
  ),
  item('Tiền cọc', '/student/deposit', WalletOutlined),
  item('Kỳ hè', '/student/summer', CalendarOutlined),
  item('Thông báo', '/student/notifications', BellOutlined),
  item('Hồ sơ', '/student/profile', UserOutlined),
]

export const technicianNav: NavItem[] = [
  item('Việc của tôi', '/technician/tasks', ToolOutlined),
]

export const CHANGE_PASSWORD_PATH = '/change-password'

/** Chọn mục menu có path dài nhất khớp với đường dẫn hiện tại. */
export function findActivePath(
  items: NavItem[],
  pathname: string,
): string | undefined {
  return items
    .filter(({ path }) => pathname === path || pathname.startsWith(`${path}/`))
    .sort((a, b) => b.path.length - a.path.length)[0]?.path
}
