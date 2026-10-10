import { useQuery, useQueryClient } from '@tanstack/react-query'
import { useEffect } from 'react'
import { useLocation } from 'react-router'
import { getUnreadCount } from '../../services/api/notification'

export const unreadCountKey = ['notifications', 'unread-count'] as const

/** Số thông báo chưa đọc: gọi lại mỗi 60 giây và mỗi khi đổi trang (API Spec, FR-NOT-03). */
export function useUnreadCount() {
  const queryClient = useQueryClient()
  const { pathname } = useLocation()

  // useQuery đặt trước effect: lần mount đã có request đang chạy, và `cancelRefetch: false` làm effect dùng lại nó
  // thay vì hủy rồi gửi thêm một request nữa
  const query = useQuery({
    queryKey: unreadCountKey,
    queryFn: getUnreadCount,
    refetchInterval: 60_000,
  })

  useEffect(() => {
    void queryClient.refetchQueries({ queryKey: unreadCountKey }, { cancelRefetch: false })
  }, [pathname, queryClient])

  return query
}
