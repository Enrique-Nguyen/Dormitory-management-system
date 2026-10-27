import {
  keepPreviousData,
  useMutation,
  useQuery,
  useQueryClient,
  type QueryClient,
} from '@tanstack/react-query'
import { useApiErrorHandler } from '../../hooks/useApiErrorHandler'
import {
  markAllRead,
  markRead,
  searchNotifications,
  type NotificationSearchQuery,
} from '../../services/api/notification'
import { unreadCountKey } from './useUnreadCount'

export const inboxKeys = {
  all: ['notifications', 'inbox'] as const,
  page: (query: NotificationSearchQuery) =>
    ['notifications', 'inbox', query] as const,
}

/** Chuẩn hóa trước khi tạo khóa cache để `q: ''` và `q: undefined` không tách thành hai trang. */
function normalizeQuery(query: NotificationSearchQuery): NotificationSearchQuery {
  return {
    ...query,
    q: query.q?.trim() || undefined,
    sort: query.sort?.length ? [...query.sort] : undefined,
  }
}

/** Hộp thư phân trang/tìm kiếm tại API; giữ dữ liệu trang cũ trong lúc tải trang mới. */
export function useInbox(query: NotificationSearchQuery) {
  const normalized = normalizeQuery(query)
  return useQuery({
    queryKey: inboxKeys.page(normalized),
    queryFn: () => searchNotifications(normalized),
    placeholderData: keepPreviousData,
  })
}

/** Đánh dấu đọc hoặc đọc tất cả phải làm mới cả danh sách lẫn số chưa đọc trên chuông. */
function invalidateInbox(queryClient: QueryClient) {
  void queryClient.invalidateQueries({ queryKey: inboxKeys.all })
  void queryClient.invalidateQueries({ queryKey: unreadCountKey })
}

/** Đánh dấu một thông báo là đã đọc. */
export function useMarkRead() {
  const queryClient = useQueryClient()
  const onError = useApiErrorHandler()
  return useMutation({
    mutationFn: (notificationId: number) => markRead(notificationId),
    onSuccess: () => invalidateInbox(queryClient),
    onError,
  })
}

/** Đánh dấu toàn bộ thông báo của người đang đăng nhập là đã đọc. */
export function useMarkAllRead() {
  const queryClient = useQueryClient()
  const onError = useApiErrorHandler()
  return useMutation({
    mutationFn: () => markAllRead(),
    onSuccess: () => invalidateInbox(queryClient),
    onError,
  })
}
