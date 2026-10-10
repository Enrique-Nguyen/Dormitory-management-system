/** Mọi API chi tiết trả `allowedActions` (API Spec 2.6, UI 27.2). UI chỉ hiện nút có trong danh sách. */
export interface WithActions {
  allowedActions: string[]
}

/** Phân trang của danh sách (API Spec 2.5); `page` bắt đầu từ 0. */
export interface Page<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}
