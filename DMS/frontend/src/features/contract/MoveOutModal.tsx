import { App } from 'antd'
import dayjs, { type Dayjs } from 'dayjs'
import { useMemo, useState } from 'react'
import {
  Alert,
  Button,
  Checkbox,
  DataTable,
  DatePicker,
  Descriptions,
  FormField,
  Modal,
  PageSkeleton,
  Radio,
  RadioGroup,
  StatusBadge,
  TextArea,
  useConfirm,
} from '../../components'
import { Permission, Role } from '../../constants/authorization'
import { hasPermission } from '../../services/api/auth'
import {
  AssetCondition,
  moveOutUtilityInvoices,
  type MoveOutInvoice,
  type MoveOutPreview,
  type MoveOutResult,
  type MoveOutTicket,
} from '../../services/api/checkout'
import { errorMessage } from '../../utils/errorMessages'
import { formatDate, formatMoney } from '../../utils/format'
import { useMe } from '../account/useMe'
import { useExecuteMoveOut, useMoveOutPreview } from './useCheckout'

export interface MoveOutModalProps {
  open: boolean
  /** Hợp đồng ACTIVE cần trả phòng; lấy từ danh sách/chi tiết hợp đồng. */
  contractId?: number
  /** Mã hợp đồng để hiển thị; không có thì dùng #id. */
  contractCode?: string
  onClose: () => void
  onSuccess?: (result: MoveOutResult) => void
}

interface FormErrors {
  assetNote?: string
  warnings?: string
}

function buildWarningLines(preview: MoveOutPreview) {
  const lines: string[] = Array.isArray(preview.warnings)
    ? preview.warnings.filter(
        (warning): warning is string => typeof warning === 'string',
      )
    : []
  const utilityInvoices = moveOutUtilityInvoices(preview)
  if (preview.unpaidRoomFeeInvoices.length > 0)
    lines.push(
      `Còn ${preview.unpaidRoomFeeInvoices.length} hóa đơn tiền phòng chưa thanh toán.`,
    )
  if (utilityInvoices.length > 0)
    lines.push(
      `Còn ${utilityInvoices.length} hóa đơn điện nước của phòng chưa thanh toán.`,
    )
  if (preview.unsettledTickets.length > 0)
    lines.push(
      `Còn ${preview.unsettledTickets.length} ticket sửa chữa chưa kết luận chi phí.`,
    )
  if (lines.length === 0 && preview.warnings === true)
    lines.push(
      'Hệ thống ghi nhận còn cảnh báo công nợ hoặc ticket chưa kết luận. Kiểm tra lại dữ liệu trước khi tiếp tục.',
    )
  return lines
}

function InvoiceSection({
  title,
  invoices,
}: {
  title: string
  invoices: MoveOutInvoice[]
}) {
  return (
    <div style={{ marginTop: 20 }}>
      <strong>{title}</strong>
      {invoices.length === 0 ? (
        <p>Không có hóa đơn chưa thanh toán.</p>
      ) : (
        <DataTable<MoveOutInvoice>
          rowKey="id"
          size="small"
          showStt={false}
          pagination={false}
          dataSource={invoices}
          columns={[
            { title: 'Mã hóa đơn', dataIndex: 'code' },
            {
              title: 'Số tiền',
              dataIndex: 'amount',
              align: 'right',
              render: (value: number) => formatMoney(value),
            },
            {
              title: 'Hạn thanh toán',
              dataIndex: 'dueDate',
              render: (value: string | null) => formatDate(value),
            },
          ]}
        />
      )}
    </div>
  )
}

/**
 * Trả phòng (UI 22A, API 4.8): xem trước công nợ/cọc/ticket theo ngày trả, bắt buộc xác nhận
 * cảnh báo và ghi tình trạng tài sản trước khi gọi API. Chỉ đóng sau khi máy chủ trả kết quả;
 * lỗi 409 giữ nguyên hộp thoại để người dùng xem lại số liệu.
 */
export default function MoveOutModal({
  open,
  contractId,
  contractCode,
  onClose,
  onSuccess,
}: MoveOutModalProps) {
  const { message } = App.useApp()
  const { confirm, dialog } = useConfirm()
  const me = useMe()
  const [moveOutDate, setMoveOutDate] = useState<Dayjs>(() => dayjs())
  const [assetCondition, setAssetCondition] = useState<AssetCondition>(
    AssetCondition.OK,
  )
  const [assetNote, setAssetNote] = useState('')
  const [acknowledged, setAcknowledged] = useState(false)
  const [errors, setErrors] = useState<FormErrors>({})

  const dateText = moveOutDate.format('YYYY-MM-DD')
  const isStudent = me.data?.role === Role.STUDENT
  const previewQuery = useMoveOutPreview(
    contractId,
    dateText,
    open && me.data != null && !isStudent,
  )
  const preview = previewQuery.data
  const moveOut = useExecuteMoveOut()

  const canExecute =
    !isStudent && hasPermission(me.data, Permission.MOVE_OUT_CREATE)
  const warningLines = useMemo(
    () => (preview ? buildWarningLines(preview) : []),
    [preview],
  )
  const hasWarnings =
    preview?.warnings === true || warningLines.length > 0

  function resetForm() {
    setMoveOutDate(dayjs())
    setAssetCondition(AssetCondition.OK)
    setAssetNote('')
    setAcknowledged(false)
    setErrors({})
  }

  function validate() {
    const next: FormErrors = {}
    if (assetCondition === AssetCondition.DAMAGED && !assetNote.trim())
      next.assetNote =
        'Mô tả tình trạng hư hỏng tài sản khi chọn "Có hư hỏng".'
    if (assetNote.trim().length > 500)
      next.assetNote = 'Ghi chú tài sản tối đa 500 ký tự.'
    if (hasWarnings && !acknowledged)
      next.warnings = 'Cần xác nhận các cảnh báo trước khi trả phòng.'
    setErrors(next)
    return Object.keys(next).length === 0
  }

  async function submit() {
    if (!contractId || !preview || !canExecute || previewQuery.isFetching)
      return
    if (!validate()) return
    const note = assetNote.trim()
    confirm({
      title: `Xác nhận trả phòng ${contractCode ?? `#${contractId}`}`,
      description: (
        <>
          Hợp đồng kết thúc ngày {formatDate(dateText)} và chuyển sang trạng thái{' '}
          <StatusBadge entity="contract" status={preview.endStatus} />. Phân
          phòng và vai trò trưởng phòng (nếu có) sẽ được đóng trong cùng giao
          dịch.
          <br />
          Số dư cọc: <strong>{formatMoney(preview.depositBalance)}</strong>
          {' · '}Tiền hoàn dự kiến:{' '}
          <strong>{formatMoney(preview.refundAmount)}</strong>
          {preview.negativeBalanceDebt > 0 && (
            <>
              {' · '}Nợ cọc:{' '}
              <strong>{formatMoney(preview.negativeBalanceDebt)}</strong>
            </>
          )}
          <br />
          Tình trạng tài sản:{' '}
          {assetCondition === AssetCondition.DAMAGED
            ? 'Có hư hỏng'
            : 'Bình thường'}
          .
          {hasWarnings && (
            <>
              <br />
              Bạn đã đọc và xác nhận {warningLines.length} cảnh báo trước khi
              tiếp tục.
            </>
          )}
        </>
      ),
      okText: 'Trả phòng',
      danger: true,
      onConfirm: async () => {
        const result = await moveOut.mutateAsync({
          contractId,
          moveOutDate: dateText,
          assetCondition,
          ...(note ? { assetNote: note } : {}),
          acknowledgedWarnings: hasWarnings ? acknowledged : true,
        })
        message.success('Đã hoàn tất trả phòng.')
        onSuccess?.(result)
        onClose()
      },
    })
  }

  const title = `Trả phòng hợp đồng ${contractCode ?? (contractId ? `#${contractId}` : '')}`

  return (
    <>
      <Modal
        title={title}
        open={open}
        onCancel={() => {
          if (!moveOut.isPending) onClose()
        }}
        footer={[
          <Button key="cancel" onClick={onClose} disabled={moveOut.isPending}>
            Hủy
          </Button>,
          <Button
            key="submit"
            type="primary"
            danger
            loading={moveOut.isPending}
            disabled={
              !preview ||
              previewQuery.isFetching ||
              !canExecute ||
              (hasWarnings && !acknowledged)
            }
            onClick={() => void submit()}
          >
            Xác nhận trả phòng
          </Button>,
        ]}
        width={960}
        mask={{ closable: false }}
        keyboard={!moveOut.isPending}
        closable={!moveOut.isPending}
        afterOpenChange={(visible) => {
          if (visible) resetForm()
        }}
      >
        {!contractId ? (
          <Alert
            type="warning"
            showIcon
            title="Chưa xác định hợp đồng"
            description="Mở chức năng trả phòng từ danh sách hoặc chi tiết hợp đồng đang ACTIVE."
          />
        ) : (
          <>
            <FormField
              label="Ngày trả phòng"
              required
              hint="Ngày trước hạn kết thúc hợp đồng sẽ chấm dứt trước hạn; từ hạn trở đi là hết hạn."
            >
              {(control) => (
                <DatePicker
                  {...control}
                  style={{ width: '100%' }}
                  value={moveOutDate}
                  onChange={(value) => {
                    if (!value) return
                    setMoveOutDate(value as Dayjs)
                    // Đổi ngày làm thay đổi công nợ/cảnh báo: bỏ xác nhận cũ.
                    setAcknowledged(false)
                    setErrors((current) => ({
                      ...current,
                      warnings: undefined,
                    }))
                  }}
                />
              )}
            </FormField>

            {(previewQuery.isLoading || me.isLoading) && (
              <div style={{ marginTop: 16 }}>
                <PageSkeleton rows={4} />
              </div>
            )}

            {previewQuery.isError && (
              <Alert
                type="error"
                showIcon
                style={{ marginTop: 16 }}
                title="Không thể xem trước trả phòng"
                description={errorMessage(previewQuery.error)}
              />
            )}

            {!canExecute && (
              <Alert
                type="warning"
                showIcon
                style={{ marginTop: 16 }}
                title="Bạn không có quyền thực hiện trả phòng"
                description={
                  isStudent
                    ? 'Sinh viên không thực hiện trả phòng trên cổng quản lý.'
                    : 'Cần quyền MOVE_OUT_CREATE. Dữ liệu xem trước vẫn hiển thị để đối chiếu.'
                }
              />
            )}

            {preview && (
              <>
                <Descriptions
                  bordered
                  size="small"
                  column={{ xs: 1, sm: 2 }}
                  style={{ marginTop: 16 }}
                  items={[
                    {
                      key: 'status',
                      label: 'Kiểu kết thúc',
                      children: (
                        <StatusBadge
                          entity="contract"
                          status={preview.endStatus}
                        />
                      ),
                    },
                    {
                      key: 'leader',
                      label: 'Trưởng phòng',
                      children: preview.isRoomLeader
                        ? 'Có — sẽ tự gỡ khi trả phòng'
                        : 'Không',
                    },
                    {
                      key: 'deposit',
                      label: 'Số dư cọc',
                      children: formatMoney(preview.depositBalance),
                    },
                    {
                      key: 'refund',
                      label: 'Tiền hoàn dự kiến',
                      children: formatMoney(preview.refundAmount),
                    },
                    {
                      key: 'debt',
                      label: 'Nợ cọc',
                      children: formatMoney(preview.negativeBalanceDebt),
                    },
                  ]}
                />

                {preview.negativeBalanceDebt > 0 && (
                  <Alert
                    type="warning"
                    showIcon
                    style={{ marginTop: 16 }}
                    title="Số dư cọc đang âm"
                    description="Sinh viên còn nợ cọc; lần trả phòng này không tạo giao dịch hoàn cọc."
                  />
                )}
                {preview.isRoomLeader && (
                  <Alert
                    type="info"
                    showIcon
                    style={{ marginTop: 16 }}
                    title="Sinh viên đang là trưởng phòng"
                    description="Vai trò trưởng phòng sẽ được gỡ tự động trong cùng giao dịch trả phòng."
                  />
                )}

                <InvoiceSection
                  title="Hóa đơn tiền phòng chưa thanh toán"
                  invoices={preview.unpaidRoomFeeInvoices}
                />
                <InvoiceSection
                  title="Hóa đơn điện nước chưa thanh toán của phòng"
                  invoices={moveOutUtilityInvoices(preview)}
                />

                <div style={{ marginTop: 20 }}>
                  <strong>Ticket chưa kết luận chi phí</strong>
                  {preview.unsettledTickets.length === 0 ? (
                    <p>Không có ticket sửa chữa chưa kết luận.</p>
                  ) : (
                    <DataTable<MoveOutTicket>
                      rowKey="id"
                      size="small"
                      showStt={false}
                      pagination={false}
                      dataSource={preview.unsettledTickets}
                      columns={[
                        { title: 'Mã ticket', dataIndex: 'code' },
                        {
                          title: 'Trạng thái',
                          dataIndex: 'status',
                          render: (value: string) => (
                            <StatusBadge entity="ticket" status={value} />
                          ),
                        },
                        {
                          title: 'Kết luận',
                          dataIndex: 'concluded',
                          render: (value: boolean) =>
                            value ? 'Đã kết luận' : 'Chưa kết luận',
                        },
                      ]}
                    />
                  )}
                </div>

                <div style={{ marginTop: 20 }}>
                  <strong>Cảnh báo</strong>
                  {hasWarnings ? (
                    <Alert
                      type="warning"
                      showIcon
                      style={{ marginTop: 8 }}
                      title="Cần xác nhận trước khi trả phòng"
                      description={
                        <ul style={{ margin: 0, paddingInlineStart: 20 }}>
                          {warningLines.map((line, index) => (
                            <li key={`${index}-${line}`}>{line}</li>
                          ))}
                        </ul>
                      }
                    />
                  ) : (
                    <p>Không có cảnh báo.</p>
                  )}
                </div>

                <div style={{ marginTop: 20 }}>
                  <FormField label="Tình trạng tài sản" required>
                    {(control) => (
                      <RadioGroup
                        {...control}
                        value={assetCondition}
                        onChange={(event) => {
                          const value = event.target.value as AssetCondition
                          setAssetCondition(value)
                          if (value === AssetCondition.OK) {
                            setAssetNote('')
                            setErrors((current) => ({
                              ...current,
                              assetNote: undefined,
                            }))
                          }
                        }}
                      >
                        <Radio value={AssetCondition.OK}>Bình thường</Radio>
                        <Radio value={AssetCondition.DAMAGED}>
                          Có hư hỏng
                        </Radio>
                      </RadioGroup>
                    )}
                  </FormField>

                  {assetCondition === AssetCondition.DAMAGED && (
                    <div style={{ marginTop: 16 }}>
                      <FormField
                        label="Ghi chú hư hỏng"
                        required
                        error={errors.assetNote}
                        hint="Tối đa 500 ký tự. Hư hỏng ghi tại đây không tự trừ cọc."
                      >
                        {(control) => (
                          <TextArea
                            {...control}
                            rows={3}
                            maxLength={500}
                            showCount
                            value={assetNote}
                            onChange={(event) =>
                              setAssetNote(event.target.value)
                            }
                            placeholder="Ví dụ: gãy chân ghế, vỡ kính cửa sổ..."
                          />
                        )}
                      </FormField>
                    </div>
                  )}

                  {hasWarnings && (
                    <div style={{ marginTop: 16 }}>
                      <Checkbox
                        checked={acknowledged}
                        onChange={(event) => {
                          setAcknowledged(event.target.checked)
                          if (event.target.checked)
                            setErrors((current) => ({
                              ...current,
                              warnings: undefined,
                            }))
                        }}
                      >
                        Tôi đã đọc và xác nhận các cảnh báo nêu trên.
                      </Checkbox>
                      {errors.warnings && (
                        <div
                          role="alert"
                          className="dms-field-error"
                          style={{ marginTop: 4 }}
                        >
                          {errors.warnings}
                        </div>
                      )}
                    </div>
                  )}
                </div>
              </>
            )}
          </>
        )}
      </Modal>
      {dialog}
    </>
  )
}
