// oxlint-disable react/set-state-in-effect -- modal mở lại phải reset form đã hoàn tất
import { App } from 'antd'
import type { TableColumnsType } from 'antd'
import { useEffect, useMemo, useState } from 'react'
import {
  Alert,
  Checkbox,
  DataTable,
  FormField,
  InputNumber,
  Modal,
  Radio,
  RadioGroup,
  Skeleton,
  TextArea,
} from '../../components'
import type {
  ChargeableStudent,
  MaintenanceCause as MaintenanceCauseCode,
  MaintenanceStudentRef,
} from '../../services/api/maintenance'
import { errorMessage } from '../../utils/errorMessages'
import { formatMoney } from '../../utils/format'
import {
  MaintenanceCause,
  maintenanceCauseLabels,
} from './constants'
import { useChargeableStudents, useConcludeTicket } from './useMaintenance'

interface Props {
  ticketId: number
  open: boolean
  onClose: () => void
}

interface FormErrors {
  repairCost?: string
  note?: string
  students?: string
}

type ShareRow = ChargeableStudent & { amount: number }

const shareColumns: TableColumnsType<ShareRow> = [
  {
    title: 'Sinh viên',
    dataIndex: 'student',
    render: (student: MaintenanceStudentRef) =>
      `${student.fullName} (${student.studentCode})`,
  },
  {
    title: 'Hợp đồng',
    dataIndex: 'contractId',
    render: (value: number) => `#${value}`,
  },
  {
    title: 'Số tiền trừ',
    dataIndex: 'amount',
    align: 'right',
    render: (value: number) => formatMoney(value),
  },
]

/**
 * Kết luận ticket CLOSED: WEAR bắt buộc ghi chú, MISUSE bắt buộc chi phí và ít nhất
 * một sinh viên. Bỏ bớt sinh viên phải giải thích; số tiền chia theo hợp đồng tăng dần,
 * người cuối nhận phần dư đúng như backend.
 */
export default function ConcludeTicketModal({
  ticketId,
  open,
  onClose,
}: Props) {
  const { message } = App.useApp()
  const [cause, setCause] = useState<MaintenanceCauseCode>(
    MaintenanceCause.WEAR,
  )
  const [repairCost, setRepairCost] = useState<number | null>(null)
  const [selectedIds, setSelectedIds] = useState<number[]>([])
  const [note, setNote] = useState('')
  const [errors, setErrors] = useState<FormErrors>({})

  const chargeable = useChargeableStudents(
    ticketId,
    open && cause === MaintenanceCause.MISUSE,
  )
  const conclude = useConcludeTicket(ticketId)

  const candidates = useMemo(
    () =>
      [...(chargeable.data ?? [])].sort(
        (left, right) => left.contractId - right.contractId,
      ),
    [chargeable.data],
  )
  const selectedCandidates = useMemo(
    () =>
      candidates.filter((candidate) =>
        selectedIds.includes(candidate.student.id),
      ),
    [candidates, selectedIds],
  )

  const allSelected =
    candidates.length > 0 && selectedCandidates.length === candidates.length
  const subsetSelected =
    selectedCandidates.length > 0 &&
    selectedCandidates.length < candidates.length
  const noteRequired =
    cause === MaintenanceCause.WEAR ||
    (cause === MaintenanceCause.MISUSE && subsetSelected)

  // oxlint-disable-next-line react/set-state-in-effect
  useEffect(() => {
    if (!open) return
    setCause(MaintenanceCause.WEAR)
    setRepairCost(null)
    setSelectedIds([])
    setNote('')
    setErrors({})
  }, [open])

  // oxlint-disable-next-line react/set-state-in-effect
  useEffect(() => {
    if (open && cause === MaintenanceCause.MISUSE && chargeable.data)
      setSelectedIds(chargeable.data.map((item) => item.student.id))
  }, [open, cause, chargeable.data])

  const shares = useMemo(() => {
    if (
      cause !== MaintenanceCause.MISUSE ||
      repairCost == null ||
      repairCost <= 0 ||
      selectedCandidates.length === 0
    )
      return []
    const count = selectedCandidates.length
    const base = Math.floor(repairCost / count)
    const remainder = repairCost % count
    return selectedCandidates.map((candidate, index) => ({
      ...candidate,
      amount: base + (index === count - 1 ? remainder : 0),
    }))
  }, [cause, repairCost, selectedCandidates])

  const toggleStudent = (studentId: number, checked: boolean) => {
    setSelectedIds((current) =>
      checked
        ? current.includes(studentId)
          ? current
          : [...current, studentId]
        : current.filter((id) => id !== studentId),
    )
    setErrors((current) => ({ ...current, students: undefined }))
  }

  const submit = async () => {
    const next: FormErrors = {}
    if (cause === MaintenanceCause.MISUSE) {
      if (
        repairCost == null ||
        !Number.isSafeInteger(repairCost) ||
        repairCost <= 0
      )
        next.repairCost = 'Nhập chi phí sửa chữa là số nguyên lớn hơn 0.'
      else if (
        selectedCandidates.length > 0 &&
        repairCost < selectedCandidates.length
      )
        next.repairCost = `Chi phí phải lớn hơn hoặc bằng số sinh viên được chọn (tối thiểu ${formatMoney(
          selectedCandidates.length,
        )}) để mỗi người nhận khoản trừ dương.`
      if (selectedCandidates.length === 0)
        next.students = 'Chọn ít nhất một sinh viên để trừ cọc.'
    }
    if (noteRequired && note.trim().length < 5)
      next.note =
        cause === MaintenanceCause.WEAR
          ? 'Mô tả tình trạng hao mòn tự nhiên (ít nhất 5 ký tự).'
          : 'Giải thích vì sao chỉ trừ cọc một số sinh viên (ít nhất 5 ký tự).'
    if (note.trim().length > 500)
      next.note = 'Ghi chú tối đa 500 ký tự.'
    setErrors(next)
    if (Object.keys(next).length > 0) return

    try {
      await conclude.mutateAsync({
        cause,
        ...(cause === MaintenanceCause.MISUSE
          ? {
              repairCost: repairCost ?? undefined,
              chargedStudentIds: selectedCandidates.map(
                (candidate) => candidate.student.id,
              ),
              note: note.trim() || undefined,
            }
          : { note: note.trim() }),
      })
      void message.success('Đã kết luận xử lý ticket.')
      onClose()
    } catch {
      // useApiErrorHandler đã hiện câu lỗi; giữ modal để người dùng sửa.
    }
  }

  return (
    <Modal
      title="Kết luận xử lý ticket"
      open={open}
      width={760}
      onCancel={onClose}
      onOk={() => void submit()}
      confirmLoading={conclude.isPending}
      okText="Xác nhận kết luận"
      cancelText="Hủy"
      cancelButtonProps={{ disabled: conclude.isPending }}
      okButtonProps={{
        disabled:
          cause === MaintenanceCause.MISUSE &&
          (chargeable.isLoading || candidates.length === 0),
      }}
      closable={!conclude.isPending}
      mask={{ closable: false }}
      destroyOnHidden
    >
      <Alert
        type="info"
        showIcon
        style={{ marginBottom: 16 }}
        title={
          cause === MaintenanceCause.WEAR
            ? 'Hao mòn tự nhiên: không trừ cọc, bắt buộc ghi chú tình trạng.'
            : 'Do sử dụng sai: chi phí sửa chữa được trừ vào cọc của sinh viên được chọn.'
        }
      />

      <div className="dms-field">
        <div className="dms-field-label">
          Nguyên nhân <span className="dms-field-required">*</span>
        </div>
        <RadioGroup
          value={cause}
          onChange={(event) => {
            setCause(event.target.value as MaintenanceCauseCode)
            setErrors({})
          }}
        >
          <Radio value={MaintenanceCause.WEAR}>
            {maintenanceCauseLabels[MaintenanceCause.WEAR]}
          </Radio>
          <Radio value={MaintenanceCause.MISUSE}>
            {maintenanceCauseLabels[MaintenanceCause.MISUSE]}
          </Radio>
        </RadioGroup>
      </div>

      {cause === MaintenanceCause.MISUSE ? (
        <div style={{ marginTop: 'var(--space-4)' }}>
          <FormField
            label="Chi phí sửa chữa"
            required
            error={errors.repairCost}
            hint="Số tiền được chia đều cho các sinh viên được chọn; phần dư cộng vào sinh viên cuối danh sách theo hợp đồng."
          >
            {(fieldControl) => (
              <InputNumber
                {...fieldControl}
                min={1}
                precision={0}
                step={1000}
                addonAfter="đ"
                style={{ width: '100%' }}
                value={repairCost}
                onChange={(value) => {
                  setRepairCost(typeof value === 'number' ? value : null)
                  setErrors((current) => ({
                    ...current,
                    repairCost: undefined,
                  }))
                }}
              />
            )}
          </FormField>

          <div className="dms-field" style={{ marginTop: 'var(--space-4)' }}>
            <div className="dms-field-label">
              Sinh viên bị trừ cọc <span className="dms-field-required">*</span>
            </div>
            {chargeable.isError ? (
              <Alert
                type="error"
                showIcon
                title="Không tải được danh sách sinh viên"
                description={errorMessage(chargeable.error)}
              />
            ) : chargeable.isLoading ? (
              <Skeleton active paragraph={{ rows: 2 }} />
            ) : candidates.length === 0 ? (
              <Alert
                type="warning"
                showIcon
                title="Không có sinh viên nào đang ở tại thời điểm tạo ticket."
                description="Không thể trừ cọc cho ticket này. Chọn Hao mòn tự nhiên nếu không quy trách nhiệm."
              />
            ) : (
              <>
                <Checkbox
                  checked={allSelected}
                  indeterminate={subsetSelected}
                  onChange={(event) =>
                    setSelectedIds(
                      event.target.checked
                        ? candidates.map((item) => item.student.id)
                        : [],
                    )
                  }
                >
                  Chọn tất cả ({candidates.length} sinh viên)
                </Checkbox>
                <div
                  style={{
                    display: 'grid',
                    gap: 'var(--space-2)',
                    marginTop: 'var(--space-2)',
                  }}
                >
                  {candidates.map((candidate) => (
                    <Checkbox
                      key={candidate.student.id}
                      checked={selectedIds.includes(candidate.student.id)}
                      onChange={(event) =>
                        toggleStudent(
                          candidate.student.id,
                          event.target.checked,
                        )
                      }
                    >
                      {candidate.student.fullName} (
                      {candidate.student.studentCode}) · Hợp đồng #
                      {candidate.contractId}
                    </Checkbox>
                  ))}
                </div>
                {errors.students ? (
                  <div role="alert" className="dms-field-error">
                    {errors.students}
                  </div>
                ) : null}
              </>
            )}
          </div>

          {subsetSelected ? (
            <Alert
              type="warning"
              showIcon
              style={{ marginTop: 'var(--space-3)' }}
              title="Chỉ trừ cọc một phần sinh viên"
              description="Cần ghi chú giải thích vì sao những sinh viên còn lại không bị trừ cọc."
            />
          ) : null}

          {repairCost != null && repairCost > 0 && selectedCandidates.length > 0 ? (
            <div style={{ marginTop: 'var(--space-4)' }}>
              <Alert
                type="info"
                showIcon
                title={`Tổng chi phí ${formatMoney(repairCost)} · Chia cho ${selectedCandidates.length} sinh viên`}
                description={
                  repairCost % selectedCandidates.length === 0
                    ? 'Mỗi sinh viên chịu số tiền bằng nhau.'
                    : `Phần dư ${formatMoney(repairCost % selectedCandidates.length)} cộng vào sinh viên cuối danh sách.`
                }
              />
              <div style={{ marginTop: 'var(--space-3)' }}>
                <DataTable<ShareRow>
                  dataSource={shares}
                  columns={shareColumns}
                  rowKey={(row) => row.student.id}
                  pagination={false}
                  showStt={false}
                  size="small"
                />
              </div>
            </div>
          ) : null}
        </div>
      ) : null}

      <div style={{ marginTop: 'var(--space-4)' }}>
        <FormField
          label="Ghi chú kết luận"
          required={noteRequired}
          error={errors.note}
          hint={
            noteRequired
              ? 'Bắt buộc mô tả tình trạng hoặc lý do chỉ trừ một phần sinh viên.'
              : 'Không bắt buộc khi trừ cọc toàn bộ sinh viên.'
          }
        >
          {(fieldControl) => (
            <TextArea
              {...fieldControl}
              rows={3}
              maxLength={500}
              showCount
              value={note}
              onChange={(event) => {
                setNote(event.target.value)
                setErrors((current) => ({ ...current, note: undefined }))
              }}
              placeholder="Tình trạng tài sản, căn cứ quy trách nhiệm..."
            />
          )}
        </FormField>
      </div>
    </Modal>
  )
}
