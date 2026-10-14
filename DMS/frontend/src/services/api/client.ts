import axios, { type InternalAxiosRequestConfig } from 'axios'

/** Lỗi nghiệp vụ backend trả về (API Spec mục 5, UI 27.2). */
export interface ApiError {
  errorCode: string
  message: string
  details?: Record<string, unknown>
  requestId?: string
}

// Phiên bằng cookie HttpOnly; Axios tự đọc cookie XSRF-TOKEN và gửi header X-XSRF-TOKEN (SAD 7.1)
export const api = axios.create({
  baseURL: '/api',
  withCredentials: true,
  xsrfCookieName: 'XSRF-TOKEN',
  xsrfHeaderName: 'X-XSRF-TOKEN',
})

const WRITE_METHODS = new Set(['post', 'put', 'patch', 'delete'])

// Cookie XSRF-TOKEN có thể thiếu/hết hạn (tải lại trang, đổi phiên). Backend từ chối ghi bằng 403 FORBIDDEN
// trước khi chạy nghiệp vụ nên làm mới token rồi gửi lại đúng một lần là an toàn, thay cho việc gọi
// /auth/csrf trước mỗi lần ghi.
type RetryableConfig = InternalAxiosRequestConfig & { _csrfRetried?: boolean }

// 401 → /login, quay lại đúng trang cũ sau khi đăng nhập (UI 22C). Lần tải đầu (/auth/me) chưa có phiên
// nên không báo "hết hạn"; các cuộc gọi sau đó mới là phiên hết hạn thật.
api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const config = axios.isAxiosError(error)
      ? (error.config as RetryableConfig | undefined)
      : undefined
    if (
      axios.isAxiosError<ApiError>(error) &&
      config &&
      !config._csrfRetried &&
      WRITE_METHODS.has((config.method ?? '').toLowerCase()) &&
      error.response?.status === 403 &&
      error.response.data?.errorCode === 'FORBIDDEN'
    ) {
      config._csrfRetried = true
      await api.get('/auth/csrf')
      return api.request(config)
    }
    if (
      axios.isAxiosError(error) &&
      error.response?.status === 401 &&
      window.location.pathname !== '/login'
    ) {
      const back = encodeURIComponent(
        window.location.pathname + window.location.search,
      )
      const expired = error.config?.url !== '/auth/me' ? '&reason=expired' : ''
      window.location.assign(`/login?redirect=${back}${expired}`)
    }
    return Promise.reject(error)
  },
)
