import { Button } from './Surface'
import { Flex } from 'antd'
import type { ReactNode } from 'react'
import type { ConfirmOptions } from './ConfirmDialog'
import { useConfirm } from './useConfirm'

export interface ActionDef {
  label: string
  /** Nhận lý do nếu có `confirm.reasonRequired`. */
  onAction: (reason?: string) => void | Promise<void>
  icon?: ReactNode
  danger?: boolean
  /** Nút chính; mặc định là nút hiển thị cuối cùng nếu không phải danger. */
  primary?: boolean
  loading?: boolean
  /** Hiện nút nhưng chưa cho bấm (ví dụ chưa chọn giường); không thay allowedActions. */
  disabled?: boolean
  /** Có thì bấm sẽ mở ConfirmDialog trước khi gọi onAction (Rule 04, 05). */
  confirm?: Omit<ConfirmOptions, 'onConfirm'>
}

interface Props {
  /** `allowedActions` từ API chi tiết. Nút nào không có trong danh sách thì không render (Rule 02). */
  allowedActions: string[]
  /** Khai báo theo thứ tự hiển thị, khóa = mã trong allowedActions. */
  actions: Record<string, ActionDef>
}

/**
 * Chỉ hiện các nút backend cho phép ở trạng thái hiện tại (UI 27.2). Không viết lại bảng chuyển trạng thái;
 * backend vẫn kiểm tra lại và trả 409 INVALID_STATE_TRANSITION nếu trạng thái đã đổi.
 */
export default function ActionBar({ allowedActions, actions }: Props) {
  const { confirm, dialog } = useConfirm()
  const visible = Object.entries(actions).filter(([code]) =>
    allowedActions.includes(code),
  )
  if (visible.length === 0) return null

  return (
    <>
      <Flex wrap gap={8} justify="flex-end">
        {visible.map(([code, def], index) => (
          <Button
            key={code}
            icon={def.icon}
            danger={def.danger}
            loading={def.loading}
            disabled={def.disabled}
            type={
              (def.primary ?? (index === visible.length - 1 && !def.danger))
                ? 'primary'
                : 'default'
            }
            onClick={() =>
              def.confirm
                ? confirm({
                    ...def.confirm,
                    danger: def.confirm.danger ?? def.danger,
                    onConfirm: def.onAction,
                  })
                : def.onAction()
            }
          >
            {def.label}
          </Button>
        ))}
      </Flex>
      {dialog}
    </>
  )
}
