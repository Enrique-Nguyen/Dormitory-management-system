import { useQuery } from '@tanstack/react-query'
import { getMe } from '../../services/api/auth'

export const meQueryKey = ['me'] as const

/** Người dùng hiện tại. Lỗi 401 được interceptor ở services/api/client.ts chuyển về /login. */
export function useMe() {
  return useQuery({
    queryKey: meQueryKey,
    queryFn: getMe,
    retry: false,
    // Quyền vai trò có thể được Admin cập nhật lúc đang đăng nhập: tải lại định kỳ và khi quay lại tab.
    // Backend kiểm tra quyền mới ở mỗi request; thời gian này chỉ quyết định lúc menu/nút được cập nhật.
    staleTime: 30 * 1000,
    refetchInterval: (query) => (query.state.data ? 30 * 1000 : false),
    refetchOnWindowFocus: true,
  })
}
