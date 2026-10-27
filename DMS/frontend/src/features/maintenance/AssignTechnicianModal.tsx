import { App } from 'antd'
import { useEffect } from 'react'
import { Controller, useForm } from 'react-hook-form'
import {
  Alert,
  FormField,
  Modal,
  Select,
} from '../../components'
import { errorMessage } from '../../utils/errorMessages'
import { useAssignTechnician, useTechnicians } from './useMaintenance'

interface AssignForm {
  technicianId?: number
}

interface Props {
  ticketId: number
  open: boolean
  onClose: () => void
}

/** Chọn kỹ thuật viên cùng đơn vị; backend kiểm tra lại TECHNICIAN_OUT_OF_UNIT. */
export default function AssignTechnicianModal({
  ticketId,
  open,
  onClose,
}: Props) {
  const { message } = App.useApp()
  const technicians = useTechnicians(open)
  const assign = useAssignTechnician(ticketId)
  const {
    control,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<AssignForm>({ defaultValues: { technicianId: undefined } })

  useEffect(() => {
    if (open) reset({ technicianId: undefined })
  }, [open, reset])

  const options = (technicians.data ?? []).map((technician) => ({
    value: technician.id,
    label: technician.fullName
      ? `${technician.fullName} (${technician.username})`
      : technician.username,
  }))

  const submit = handleSubmit(async ({ technicianId }) => {
    if (technicianId == null) return
    try {
      await assign.mutateAsync(technicianId)
      void message.success('Đã giao ticket cho kỹ thuật viên.')
      onClose()
    } catch {
      // useApiErrorHandler đã hiện câu lỗi; giữ hộp thoại để chọn lại.
    }
  })

  return (
    <Modal
      title="Giao ticket cho kỹ thuật viên"
      open={open}
      width={560}
      onCancel={onClose}
      onOk={() => void submit()}
      confirmLoading={assign.isPending}
      okText="Giao việc"
      cancelText="Hủy"
      cancelButtonProps={{ disabled: assign.isPending }}
      okButtonProps={{ disabled: options.length === 0 }}
      destroyOnHidden
      mask={{ closable: false }}
    >
      <Alert
        type="info"
        showIcon
        style={{ marginBottom: 16 }}
        title="Chỉ kỹ thuật viên đang hoạt động trong đơn vị của ticket nhận được việc."
      />
      {technicians.isError ? (
        <Alert
          type="error"
          showIcon
          style={{ marginBottom: 16 }}
          title="Không tải được danh sách kỹ thuật viên"
          description={errorMessage(technicians.error)}
        />
      ) : null}
      {!technicians.isLoading && options.length === 0 ? (
        <Alert
          type="warning"
          showIcon
          style={{ marginBottom: 16 }}
          title="Chưa có kỹ thuật viên đang hoạt động trong đơn vị."
          description="Liên hệ quản trị viên để bổ sung tài khoản kỹ thuật viên trước khi giao việc."
        />
      ) : null}
      <FormField
        label="Kỹ thuật viên"
        required
        error={errors.technicianId?.message}
      >
        {(fieldControl) => (
          <Controller
            name="technicianId"
            control={control}
            rules={{ required: 'Chọn kỹ thuật viên phụ trách.' }}
            render={({ field }) => (
              <Select
                {...fieldControl}
                value={field.value}
                onChange={field.onChange}
                onBlur={field.onBlur}
                options={options}
                loading={technicians.isLoading}
                showSearch
                optionFilterProp="label"
                allowClear
                placeholder="Tìm theo tên hoặc tên đăng nhập"
                notFoundContent="Không có kỹ thuật viên phù hợp"
              />
            )}
          />
        )}
      </FormField>
    </Modal>
  )
}
