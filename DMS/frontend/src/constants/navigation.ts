import { Permission, Role } from './authorization'
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

// Nguồn duy nhất cho menu và trang chủ từng vai trò (UI/UX mục 3, 4, 26).
export interface NavItem {
  key: string
  label: string
  path: string
  icon?: ComponentType
  /** Mục ghép dùng nhiều quyền READ; có ít nhất một quyền mới hiện trong menu. */
  permissions: Permission[]
}

export interface NavGroup {
  title?: string
  items: NavItem[]
}

/** Phân biệt vai trò cần cấu hình quyền với vai trò của người đang đăng nhập. */
export const rolePermissionLabels = {
  matrixTitle: 'Ma trận quyền',
  roleSelector: 'Vai trò đang phân quyền',
  grantedCount: 'quyền được cấp',
  adminFullAccess: 'Tự động cấp toàn quyền',
} as const

export const commonUiLabels = {
  search: 'Tìm kiếm',
  emptyData: 'Chưa có dữ liệu',
} as const

const item = (label: string, path: string, icon?: ComponentType): NavItem => ({
  key: path,
  label,
  path,
  icon,
  permissions: permissionsForPath(path),
})

/**
 * Dùng chung cho menu và route; URL chi tiết vẫn lấy quyền đọc của màn danh sách.
 * Mỗi cổng có quyền trang chủ riêng. Cổng/màn chưa khai báo luôn trả danh sách rỗng để bị chặn.
 */
export function permissionsForPath(path: string): Permission[] {
  const pathname = path.split(/[?#]/)[0].replace(/\/+$/, '')
  const portal = pathname.match(
    /^\/(admin|staff|student|technician)(?:\/|$)/,
  )?.[1]
  if (!portal) return []
  const screen = pathname.slice(portal.length + 1).replace(/^\//, '')
  if (!screen) {
    const homePermissions: Record<string, Permission[]> = {
      admin: [Permission.USER_READ],
      staff: [Permission.PROFILE_READ],
      student: [Permission.PROFILE_READ],
      technician: [Permission.PROFILE_READ],
    }
    return homePermissions[portal]
  }
  const screens: Record<string, Permission[]> = {
    dashboard: [Permission.DASHBOARD_READ],
    users: [Permission.USER_READ],
    units: [Permission.MANAGEMENT_UNIT_READ],
    buildings: [Permission.BUILDING_READ],
    import: [Permission.STUDENT_READ],
    'aggregate-permissions': [Permission.USER_READ],
    'role-permissions': [Permission.ROLE_PERMISSION_READ],
    'priority-categories': [Permission.PRIORITY_CATEGORY_READ],
    configs: [Permission.CONFIG_READ],
    'audit-logs': [Permission.AUDIT_READ],
    students: [Permission.STUDENT_READ],
    applications: [Permission.ROOM_APPLICATION_READ],
    rooms: [Permission.ROOM_READ],
    room: [Permission.ROOM_READ],
    contracts: [Permission.CONTRACT_READ],
    requests: [
      Permission.CONTRACT_EXTENSION_READ,
      Permission.ROOM_TRANSFER_READ,
    ],
    summer: [Permission.SUMMER_REGISTRATION_READ],
    storage: [Permission.STORAGE_READ],
    'utilities/prices': [Permission.UTILITY_RATE_READ],
    utilities: [Permission.UTILITY_READING_READ],
    invoices: [Permission.INVOICE_READ],
    deposits: [Permission.DEPOSIT_READ],
    deposit: [Permission.DEPOSIT_READ],
    maintenance: [Permission.MAINTENANCE_READ],
    tasks: [Permission.MAINTENANCE_READ],
    notifications: [Permission.NOTIFICATION_READ],
    'room-types': [Permission.ROOM_TYPE_READ],
    semesters: [Permission.SEMESTER_READ],
    'registration-periods': [Permission.REGISTRATION_PERIOD_READ],
    profile: [Permission.PROFILE_READ],
  }
  const match = Object.keys(screens)
    .sort((left, right) => right.length - left.length)
    .find((key) => screen === key || (key && screen.startsWith(`${key}/`)))
  return match === undefined ? [] : screens[match]
}

/** Gốc cổng và màn hồ sơ dùng chung cho menu tài khoản, guard và chuyển hướng sau đăng nhập. */
export const rolePortal: Record<Role, string> = {
  [Role.STUDENT]: '/student',
  [Role.STAFF]: '/staff',
  [Role.TECHNICIAN]: '/technician',
  [Role.ADMIN]: '/admin',
}

export const roleProfile: Record<Role, string> = {
  [Role.STUDENT]: `${rolePortal[Role.STUDENT]}/profile`,
  [Role.STAFF]: `${rolePortal[Role.STAFF]}/profile`,
  [Role.TECHNICIAN]: `${rolePortal[Role.TECHNICIAN]}/profile`,
  [Role.ADMIN]: `${rolePortal[Role.ADMIN]}/profile`,
}

/** Trang chủ của từng vai trò; route ngoài quyền chuyển về đây. */
export const roleHome: Record<Role, string> = {
  [Role.STUDENT]: '/student',
  [Role.STAFF]: '/staff/dashboard',
  [Role.TECHNICIAN]: '/technician/tasks',
  [Role.ADMIN]: '/admin/users',
}

export const roleLabel: Record<Role, string> = {
  [Role.STUDENT]: 'Sinh viên',
  [Role.STAFF]: 'Quản lý',
  [Role.TECHNICIAN]: 'Kỹ thuật viên',
  [Role.ADMIN]: 'Quản trị viên',
}

/** Mục "Yêu cầu chờ xử lý" mang badge số lượng (UI 4.2). */
export const STAFF_REQUESTS_PATH = '/staff/requests'

/** Màn nghiệp vụ chi tiết (TV2): route, link từ danh sách CRUD và breadcrumb dùng chung. */
export const STAFF_ROOMS_PATH = '/staff/rooms'
export const STAFF_TRANSFERS_PATH = '/staff/transfers'
export const STUDENT_REQUESTS_PATH = '/student/requests'
export const STUDENT_TRANSFERS_PATH = `${STUDENT_REQUESTS_PATH}/transfers`

/** Danh mục phục vụ nghiệp vụ, dùng chung giữa cổng quản lý và cổng quản trị. */
const catalogNav: NavGroup = {
  title: 'Danh mục',
  items: [
    item('Loại phòng', '/staff/room-types', DatabaseOutlined),
    item('Kỳ học', '/staff/semesters', CalendarOutlined),
    item('Đợt đăng ký', '/staff/registration-periods', ProfileOutlined),
  ],
}

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
      item('Phòng & Giường', STAFF_ROOMS_PATH, HomeOutlined),
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
  catalogNav,
]

export const adminNav: NavGroup[] = [
  ...staffNav.map((group) => ({
    ...group,
    items: [
      // Các danh mục riêng của Admin cùng nằm sau nhóm nghiệp vụ.
      ...(group === catalogNav
        ? [
            item('Đơn vị', '/admin/units', BankOutlined),
            item('Tòa nhà & mức cọc', '/admin/buildings', HomeOutlined),
            item(
              'Diện ưu tiên',
              '/admin/priority-categories',
              DatabaseOutlined,
            ),
          ]
        : []),
      ...group.items.map((nav) => {
        if (nav.path === '/staff/students')
          return item('Sinh viên', '/admin/import', UploadOutlined)
        const path = nav.path.replace('/staff/', '/admin/')
        return { ...nav, key: path, path }
      }),
    ],
  })),
  {
    // Nhóm quản trị hệ thống luôn ở cuối; quyền của từng mục vẫn do backend cấp.
    title: 'Quản trị hệ thống',
    items: [
      item('Tài khoản', '/admin/users', UserOutlined),
      item('Quyền tổng hợp', '/admin/aggregate-permissions', SafetyOutlined),
      item(
        'Phân quyền theo vai trò',
        '/admin/role-permissions',
        SafetyOutlined,
      ),
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
    STUDENT_REQUESTS_PATH,
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
