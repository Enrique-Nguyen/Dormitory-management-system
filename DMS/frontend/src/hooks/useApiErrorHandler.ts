import { useQueryClient } from '@tanstack/react-query'
import { App } from 'antd'
import axios from 'axios'
import { useCallback } from 'react'
import { errorMessage } from '../utils/errorMessages'

/**
 * Dùng làm `onError` của mutation: hiện câu tiếng Việt theo errorCode và, khi gặp 409 (trạng thái đã đổi, giường
 * vừa bị lấy...), tải lại dữ liệu đang hiển thị (UI 27.1).
 */
export function useApiErrorHandler() {
  const { message } = App.useApp()
  const queryClient = useQueryClient()

  return useCallback(
    (error: unknown) => {
      message.error(errorMessage(error))
      if (axios.isAxiosError(error) && error.response?.status === 409) {
        void queryClient.invalidateQueries()
      }
    },
    [message, queryClient],
  )
}
