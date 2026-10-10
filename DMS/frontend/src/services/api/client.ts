import axios from 'axios'

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

// 401 → /login, quay lại đúng trang cũ sau khi đăng nhập (UI 22C). Lần tải đầu (/auth/me) chưa có phiên
// nên không báo "hết hạn"; các cuộc gọi sau đó mới là phiên hết hạn thật.
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (axios.isAxiosError(error) && error.response?.status === 401 && window.location.pathname !== '/login') {
      const back = encodeURIComponent(window.location.pathname + window.location.search)
      const expired = error.config?.url !== '/auth/me' ? '&reason=expired' : ''
      window.location.assign(`/login?redirect=${back}${expired}`)
    }
    return Promise.reject(error)
  },
)
