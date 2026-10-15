import axios from 'axios'
import { queryClient } from '../../app/queryClient'
import { CHANGE_PASSWORD_PATH } from '../../constants/navigation'
import { keycloak, loginPath } from './auth'

/** Lỗi nghiệp vụ backend trả về (API Spec mục 5, UI 27.2). */
export interface ApiError {
  errorCode: string
  message: string
  details?: Record<string, unknown>
  requestId?: string
}

// API xác thực bằng Bearer JWT; trình duyệt không gửi cookie phiên để đăng nhập.
export const api = axios.create({
  baseURL: '/api',
  withCredentials: false,
})

// Làm mới token trước request; không lưu token vào localStorage/sessionStorage.
api.interceptors.request.use(async (config) => {
  if (keycloak.authenticated) {
    try {
      await keycloak.updateToken(30)
    } catch {
      keycloak.clearToken()
      queryClient.clear()
      if (window.location.pathname !== '/login')
        window.location.assign(loginPath('expired'))
      throw new Error('Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.')
    }
    if (keycloak.token)
      config.headers.set('Authorization', `Bearer ${keycloak.token}`)
  }
  return config
})

/** Xóa cache nghiệp vụ; giữ kết quả /me để guard đọc được lỗi đang trả về. */
function clearBusinessCache() {
  queryClient.removeQueries({
    predicate: (query) => query.queryKey[0] !== 'me',
  })
}

let navigatingToAuthentication = false

api.interceptors.response.use(
  (response) => response,
  (error: unknown) => {
    if (!axios.isAxiosError<ApiError>(error)) return Promise.reject(error)
    const status = error.response?.status
    const code = error.response?.data?.errorCode
    const endpoint = error.config?.url?.split('?')[0]
    // Để hàm logout xử lý phản hồi thu hồi token trước khi chuyển sang Keycloak.
    if (endpoint === '/auth/logout') return Promise.reject(error)

    if (code === 'PASSWORD_CHANGE_REQUIRED') {
      clearBusinessCache()
      if (endpoint !== '/auth/me')
        void queryClient.invalidateQueries({ queryKey: ['me'] })
      if (
        window.location.pathname !== CHANGE_PASSWORD_PATH &&
        !navigatingToAuthentication
      ) {
        navigatingToAuthentication = true
        window.location.assign(CHANGE_PASSWORD_PATH)
      }
    } else if (status === 401 || code === 'ACCOUNT_LOCKED') {
      if (status === 401) keycloak.clearToken()
      clearBusinessCache()
      // /me chưa có phiên là bình thường tại login; guard xử lý lỗi khi ở cổng riêng.
      if (
        endpoint !== '/auth/me' &&
        window.location.pathname !== '/login' &&
        !navigatingToAuthentication
      ) {
        navigatingToAuthentication = true
        queryClient.clear()
        const reason =
          code === 'ACCOUNT_LOCKED'
            ? 'locked'
            : code === 'SESSION_REVOKED'
              ? 'revoked'
              : 'expired'
        window.location.assign(
          loginPath(reason, window.location.pathname + window.location.search),
        )
      }
    } else if (
      status === 403 &&
      code === 'FORBIDDEN' &&
      endpoint !== '/auth/me'
    ) {
      // Quyền có thể vừa bị thu hồi; làm mới menu/nút nhưng không tự gửi lại request đã bị từ chối.
      void queryClient.invalidateQueries({ queryKey: ['me'] })
    }
    // Không tự gửi lại thao tác ghi để tránh tạo dữ liệu trùng ngoài ý muốn.
    return Promise.reject(error)
  },
)
