import { useQuery } from '@tanstack/react-query'
import { getMe } from '../../services/api/auth'

export const meQueryKey = ['me'] as const

/** Người dùng hiện tại. Lỗi 401 được interceptor ở services/api/client.ts chuyển về /login. */
export function useMe() {
  return useQuery({
    queryKey: meQueryKey,
    queryFn: getMe,
    retry: false,
    // `student.hasActiveContract`, `isRoomLeader` đổi do người khác thao tác: chỉ cache ngắn, tải lại khi quay lại tab,
    // và bị invalidate khi gặp 409 (useApiErrorHandler)
    staleTime: 60 * 1000,
    refetchOnWindowFocus: true,
  })
}
