import { TextArea } from './FormControls'
import { Button, Modal } from './Surface'
import { zodResolver } from '@hookform/resolvers/zod'
import { Flex, Typography } from 'antd'
import { useState, type ReactNode } from 'react'
import { Controller, useForm } from 'react-hook-form'
import { z } from 'zod'
import FormField from './FormField'

// Rule 04 (hành động không hoàn tác phải confirm) và Rule 05 (lý do bắt buộc). API Spec 2.7: 5–500 ký tự sau khi trim.
const reasonSchema = z.object({
  reason: z
    .string()
    .trim()
    .min(5, 'Lý do cần ít nhất 5 ký tự.')
    .max(500, 'Lý do tối đa 500 ký tự.'),
})
type ReasonForm = z.infer<typeof reasonSchema>

export interface ConfirmOptions {
  title: ReactNode
  description?: ReactNode
  okText?: string
  danger?: boolean
  /** Hiện ô "Lý do *" bắt buộc; giá trị đã trim được truyền cho onConfirm. */
  reasonRequired?: boolean
  /** Throw để giữ dialog mở (ví dụ lỗi 409); caller tự hiển thị lỗi. */
  onConfirm: (reason?: string) => void | Promise<void>
}

interface Props extends ConfirmOptions {
  open: boolean
  onClose: () => void
}

interface BodyProps extends Omit<Props, 'open' | 'title'> {
  submitting: boolean
  setSubmitting: (value: boolean) => void
}

function Body({
  description,
  okText = 'Xác nhận',
  danger,
  reasonRequired,
  onConfirm,
  onClose,
  submitting,
  setSubmitting,
}: BodyProps) {
  const {
    control,
    handleSubmit,
    formState: { errors },
  } = useForm<ReasonForm>({
    resolver: reasonRequired ? zodResolver(reasonSchema) : undefined,
    defaultValues: { reason: '' },
  })

  const submit = handleSubmit(async ({ reason }) => {
    setSubmitting(true)
    try {
      await onConfirm(reasonRequired ? reason.trim() : undefined)
      onClose()
    } catch {
      // Giữ dialog mở để người dùng xem lỗi do caller hiển thị
    } finally {
      setSubmitting(false)
    }
  })

  return (
    <form onSubmit={submit} noValidate>
      {description && (
        <Typography.Paragraph>{description}</Typography.Paragraph>
      )}
      {reasonRequired && (
        <FormField label="Lý do" required error={errors.reason?.message}>
          {(control_) => (
            <Controller
              name="reason"
              control={control}
              render={({ field }) => (
                <TextArea
                  {...field}
                  {...control_}
                  rows={3}
                  maxLength={500}
                  placeholder="Nhập lý do..."
                />
              )}
            />
          )}
        </FormField>
      )}
      <Flex justify="flex-end" gap={8}>
        <Button onClick={onClose} disabled={submitting}>
          Hủy
        </Button>
        <Button
          type="primary"
          danger={danger}
          htmlType="submit"
          loading={submitting}
        >
          {okText}
        </Button>
      </Flex>
    </form>
  )
}

/** Dialog xác nhận, tùy chọn bắt buộc nhập lý do. Esc đóng, focus trap và trả focus do antd Modal xử lý. */
export default function ConfirmDialog({
  open,
  title,
  onClose,
  ...rest
}: Props) {
  // Đang gửi thì không cho đóng (Esc, nút X): nếu đóng giữa chừng, lỗi 409 không còn dialog để giữ mở
  const [submitting, setSubmitting] = useState(false)
  return (
    <Modal
      open={open}
      title={title}
      onCancel={() => !submitting && onClose()}
      keyboard={!submitting}
      closable={!submitting}
      footer={null}
      destroyOnHidden
      mask={{ closable: false }}
    >
      <Body
        {...rest}
        onClose={onClose}
        submitting={submitting}
        setSubmitting={setSubmitting}
      />
    </Modal>
  )
}
