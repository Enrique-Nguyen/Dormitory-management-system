import { api } from './client'

// API Spec 4.17: hộp thư của người đang đăng nhập. Phạm vi (scope) và trạng thái đọc do backend
// lọc theo danh tính hiện tại; client chỉ gửi điều kiện phân trang/tìm kiếm/sắp xếp.

/** Trang dữ liệu dùng chung của POST search-by-page (trang bắt đầu từ 0). */
export interface PageResponse<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

/** Một thông báo trong hộp thư; `link` do backend suy ra, null với thông báo do Staff đăng. */
export interface NotificationResponse {
  id: number
  scope: string
  title: string
  body: string
  source: string
  link: string | null
  createdAt: string
  isRead: boolean
}

export interface NotificationSearchQuery {
  /** Trang bắt đầu từ 0 như POST search-by-page dùng chung. */
  page: number
  /** Số bản ghi mỗi trang, backend giới hạn 1–100. */
  size: number
  q?: string
  /** Cột sắp xếp dạng `field,direction`; backend chỉ chấp nhận cột đã cho phép. */
  sort?: string[]
  /** true: chỉ chưa đọc; false: chỉ đã đọc; bỏ trống: tất cả. */
  unread?: boolean
}

// API Spec: POST /api/notifications/search-by-page → PageResponse<NotificationResponse>
export async function searchNotifications(
  query: NotificationSearchQuery,
): Promise<PageResponse<NotificationResponse>> {
  return (
    await api.post<PageResponse<NotificationResponse>>(
      '/notifications/search-by-page',
      {
        ...query,
        q: query.q?.trim() || undefined,
      },
    )
  ).data
}

// API Spec: GET /api/notifications/unread-count → { count }
export async function getUnreadCount(): Promise<number> {
  return (await api.get<{ count: number }>('/notifications/unread-count')).data
    .count
}

// API Spec: POST /api/notifications/{id}/read → 204
export async function markRead(notificationId: number): Promise<void> {
  await api.post(`/notifications/${notificationId}/read`)
}

// API Spec: POST /api/notifications/read-all → 204
export async function markAllRead(): Promise<void> {
  await api.post('/notifications/read-all')
}
