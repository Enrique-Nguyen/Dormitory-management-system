// oxlint-disable react/set-state-in-effect -- mở lại hộp thoại phải nạp lại trạng thái của diện ưu tiên
import { zodResolver } from '@hookform/resolvers/zod'
import { App } from 'antd'
import { useEffect, useState } from 'react'
import { Controller, useForm } from 'react-hook-form'
import { z } from 'zod'
import {
  Alert,
  Checkbox,
  FormField,
  Modal,
  TextArea,
} from '../../components'
import type { ApplicationPriority } from '../../services/api/registration'
import { useVerifyApplicationPriority } from './useRegistration'

// API Spec 2.7 / 4.3: xác nhận diện ưu tiên; ghi chú tùy chọn tối đa 500 ký tự sau khi trim.
const verifySchema = z.object({
  verified: z.boolean(),
  note: z.string().trim().max(500, 'Ghi chú tối đa 500 ký tự.'),
})
type VerifyForm = z.infer<typeof verifySchema>

interface Props {
  applicationId: number
  priority: ApplicationPriority
  open: boolean
  onClose: () => void
}

/**
 * Hộp thoại xác nhận một diện ưu tiên của đơn đang SUBMITTED. Bỏ chọn nghĩa là diện không đúng,
 * backend tính lại điểm và trả chi tiết mới; trang tự cập nhật nhờ hook mutation.
 */
export default function VerifyPriorityModal({
  applicationId,
  priority,
  open,
  onClose,
}: Props) {
  const { message } = App.useApp()
  const verify = useVerifyApplicationPriority(applicationId)
  const {
    control,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<VerifyForm>({
    resolver: zodResolver(verifySchema),
    defaultValues: { verified: priority.verified !== false, note: '' },
  })
  // Nhãn nút và cảnh báo đổi ngay khi bỏ chọn; giá trị gửi đi vẫn lấy từ form.
  const [accepted, setAccepted] = useState(priority.verified !== false)

  useEffect(() => {
    if (!open) return
    const verified = priority.verified !== false
    reset({ verified, note: priority.note ?? '' })
    setAccepted(verified)
  }, [open, priority, reset])

  const submit = handleSubmit(async ({ verified: accepted, note }) => {
    try {
      await verify.mutateAsync({
        categoryId: priority.categoryId,
        verified: accepted,
        note: note.trim() || undefined,
      })
      void message.success(
        accepted
          ? 'Đã xác nhận diện ưu tiên và tính lại điểm.'
          : 'Đã đánh dấu diện ưu tiên không đúng và tính lại điểm.',
      )
      onClose()
    } catch {
      // useApiErrorHandler đã hiện câu lỗi; giữ hộp thoại để người dùng thử lại.
    }
  })

  return (
    <Modal
      title="Xác nhận diện ưu tiên"
      open={open}
      width={560}
      onCancel={onClose}
      onOk={() => void submit()}
      confirmLoading={verify.isPending}
      okText={accepted ? 'Xác nhận đúng' : 'Đánh dấu không đúng'}
      cancelText="Hủy"
      cancelButtonProps={{ disabled: verify.isPending }}
      destroyOnHidden
      mask={{ closable: false }}
    >
      <p className="dms-page-subtitle" style={{ marginTop: 0 }}>
        {priority.name} · trọng số {priority.weight}
      </p>
      <FormField
        label="Kết quả xác nhận"
        required
        hint="Điểm ưu tiên của đơn được tính lại ngay sau khi lưu."
      >
        {(fieldControl) => (
          <Controller
            name="verified"
            control={control}
            render={({ field }) => (
              <Checkbox
                {...fieldControl}
                checked={field.value}
                onChange={(event) => {
                  field.onChange(event.target.checked)
                  setAccepted(event.target.checked)
                }}
              >
                Sinh viên đúng diện ưu tiên này
              </Checkbox>
            )}
          />
        )}
      </FormField>
      {!accepted && (
        <Alert
          type="warning"
          showIcon
          style={{ margin: '16px 0' }}
          title="Diện này sẽ không được tính vào điểm ưu tiên"
          description="Chỉ đánh dấu không đúng khi đã kiểm tra giấy tờ hoặc hồ sơ minh chứng của sinh viên."
        />
      )}
      <FormField label="Ghi chú xác nhận" error={errors.note?.message}>
        {(fieldControl) => (
          <Controller
            name="note"
            control={control}
            render={({ field }) => (
              <TextArea
                {...field}
                {...fieldControl}
                rows={3}
                maxLength={500}
                showCount
                placeholder="Ví dụ: đã đối chiếu giấy xác nhận hộ nghèo..."
              />
            )}
          />
        )}
      </FormField>
    </Modal>
  )
}
