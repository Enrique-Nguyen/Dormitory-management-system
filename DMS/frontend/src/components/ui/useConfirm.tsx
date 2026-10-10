import { useState } from 'react'
import ConfirmDialog, { type ConfirmOptions } from './ConfirmDialog'

/**
 * Quản lý trạng thái mở/đóng của ConfirmDialog:
 *   const { confirm, dialog } = useConfirm()
 *   confirm({ title: 'Xác nhận từ chối đơn', reasonRequired: true, danger: true, onConfirm: (reason) => reject(reason) })
 *   return <>{...}{dialog}</>
 */
export function useConfirm() {
  const [open, setOpen] = useState(false)
  // Giữ options cuối để nội dung không mất khi modal đang chạy animation đóng
  const [options, setOptions] = useState<ConfirmOptions | null>(null)

  const confirm = (next: ConfirmOptions) => {
    setOptions(next)
    setOpen(true)
  }

  const dialog = options ? <ConfirmDialog {...options} open={open} onClose={() => setOpen(false)} /> : null

  return { confirm, dialog }
}
