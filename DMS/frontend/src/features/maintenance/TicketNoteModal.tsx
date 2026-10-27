import { zodResolver } from '@hookform/resolvers/zod'
import { useEffect } from 'react'
import { Controller, useForm } from 'react-hook-form'
import { z } from 'zod'
import { FormField, Modal, TextArea } from '../../components'

// API Spec 2.7 / MaintenanceDtos.Note: 5–500 ký tự sau khi trim.
const noteSchema = z.object({
  note: z
    .string()
    .trim()
    .min(5, 'Nội dung cần ít nhất 5 ký tự.')
    .max(500, 'Nội dung tối đa 500 ký tự.'),
})
type NoteForm = z.infer<typeof noteSchema>

interface Props {
  open: boolean
  title: string
  label: string
  placeholder: string
  okText: string
  loading: boolean
  onSubmit: (note: string) => Promise<void>
  onClose: () => void
}

/** Form ghi chú dùng chung cho thêm tiến độ và hoàn thành sửa chữa. */
export default function TicketNoteModal({
  open,
  title,
  label,
  placeholder,
  okText,
  loading,
  onSubmit,
  onClose,
}: Props) {
  const {
    control,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<NoteForm>({
    resolver: zodResolver(noteSchema),
    defaultValues: { note: '' },
  })

  useEffect(() => {
    if (open) reset({ note: '' })
  }, [open, reset])

  const submit = handleSubmit(async ({ note }) => {
    try {
      await onSubmit(note.trim())
      onClose()
    } catch {
      // Mutation đã báo lỗi qua useApiErrorHandler; giữ nội dung để người dùng thử lại.
    }
  })

  return (
    <Modal
      title={title}
      open={open}
      width={560}
      onCancel={onClose}
      onOk={() => void submit()}
      confirmLoading={loading}
      okText={okText}
      cancelText="Hủy"
      cancelButtonProps={{ disabled: loading }}
      destroyOnHidden
      mask={{ closable: false }}
    >
      <FormField label={label} required error={errors.note?.message}>
        {(fieldControl) => (
          <Controller
            name="note"
            control={control}
            render={({ field }) => (
              <TextArea
                {...field}
                {...fieldControl}
                rows={4}
                maxLength={500}
                showCount
                placeholder={placeholder}
              />
            )}
          />
        )}
      </FormField>
    </Modal>
  )
}
