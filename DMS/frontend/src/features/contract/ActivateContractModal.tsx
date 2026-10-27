import { App } from 'antd'
import { useState } from 'react'
import {
  Alert,
  Button,
  Checkbox,
  FormField,
  FormGrid,
  InputNumber,
  Modal,
  Radio,
  RadioGroup,
  TextArea,
} from '../../components'
import { PaymentMethod } from '../../constants/statusConfig'
import {
  contractCodeOf,
  contractDepositInfo,
  type ActivationResult,
  type ContractDetail,
} from '../../services/api/contract'
import { formatDateTime, formatMoney } from '../../utils/format'
import { useActivateContract } from './useContract'

export interface ActivateContractModalProps {
  open: boolean
  /** Chi tiết hợp đồng PENDING; mức cọc điền sẵn từ `deposit.required`. */
  contract?: ContractDetail | null
  /** Dùng khi màn cha chỉ có ID hợp đồng. */
  contractId?: number
  /** Mức cọc bắt buộc lấy từ tòa nhà khi chi tiết PENDING chưa trả `depositRequired`. */
  requiredDeposit?: number | null
  onClose: () => void
  onSuccess?: (result: ActivationResult) => void
}

interface FormErrors {
  paperContractSigned?: string
  depositAmount?: string
  note?: string
}

/**
 * Kích hoạt hợp đồng PENDING (API 4.6, UI 22A): Staff xác nhận giấy tờ, thu đúng mức cọc của tòa
 * và chọn hình thức thanh toán. Backend kiểm tra lại mức cọc/hạn kích hoạt trong một transaction;
 * lỗi 400/409 giữ hộp thoại để người dùng đối chiếu.
 */
export default function ActivateContractModal({
  open,
  contract,
  contractId,
  requiredDeposit,
  onClose,
  onSuccess,
}: ActivateContractModalProps) {
  const { message } = App.useApp()
  const resolvedId = contract?.id ?? contractId
  const deposit = contract
    ? contractDepositInfo(contract)
    : { required: null, balance: null, topUpRequired: null, topUpOverdue: false }
  const required = requiredDeposit ?? deposit.required
  const activate = useActivateContract(resolvedId ?? 0)

  const [paperContractSigned, setPaperContractSigned] = useState(false)
  const [depositAmount, setDepositAmount] = useState<number | null>(required)
  const [paymentMethod, setPaymentMethod] = useState<string>(
    PaymentMethod.CASH,
  )
  const [note, setNote] = useState('')
  const [errors, setErrors] = useState<FormErrors>({})

  function resetForm() {
    setPaperContractSigned(false)
    setDepositAmount(deposit.required)
    setPaymentMethod(PaymentMethod.CASH)
    setNote('')
    setErrors({})
  }

  function validate() {
    const next: FormErrors = {}
    if (!paperContractSigned)
      next.paperContractSigned =
        'Cần xác nhận đã ký giấy tờ hợp đồng với sinh viên.'
    if (depositAmount == null || !Number.isFinite(depositAmount) || depositAmount <= 0)
      next.depositAmount = 'Nhập số tiền cọc đã thu.'
    else if (deposit.required != null && depositAmount !== deposit.required)
      next.depositAmount = `Mức cọc bắt buộc của tòa là ${formatMoney(deposit.required)}. Nhập đúng số tiền này.`
    if (note.trim().length > 500)
      next.note = 'Ghi chú tối đa 500 ký tự.'
    setErrors(next)
    return Object.keys(next).length === 0
  }

  async function submit() {
    if (!resolvedId || !validate() || depositAmount == null) return
    try {
      const result = await activate.mutateAsync({
        paperContractSigned,
        depositAmount,
        paymentMethod,
        ...(note.trim() ? { note: note.trim() } : {}),
      })
      message.success(
        contract
          ? `Đã kích hoạt hợp đồng ${contractCodeOf(contract)} và ghi nhận tiền cọc.`
          : 'Đã kích hoạt hợp đồng và ghi nhận tiền cọc.',
      )
      onSuccess?.(result)
      onClose()
    } catch {
      // useApiErrorHandler đã hiện câu tiếng Việt; giữ hộp thoại để sửa số tiền/xác nhận.
    }
  }

  const title = contract
    ? `Kích hoạt hợp đồng ${contractCodeOf(contract)}`
    : 'Kích hoạt hợp đồng'

  return (
    <Modal
      title={title}
      open={open}
      onCancel={() => {
        if (!activate.isPending) onClose()
      }}
      footer={[
        <Button key="cancel" onClick={onClose} disabled={activate.isPending}>
          Hủy
        </Button>,
        <Button
          key="submit"
          type="primary"
          loading={activate.isPending}
          disabled={!resolvedId || !paperContractSigned}
          onClick={() => void submit()}
        >
          Xác nhận kích hoạt
        </Button>,
      ]}
      width={720}
      mask={{ closable: false }}
      keyboard={!activate.isPending}
      closable={!activate.isPending}
      afterOpenChange={(visible) => {
        if (visible) resetForm()
      }}
    >
      {!contract ? (
        <Alert
          type="warning"
          showIcon
          title="Chưa tải được chi tiết hợp đồng"
          description="Mở chức năng kích hoạt từ chi tiết hợp đồng PENDING để điền sẵn mức cọc bắt buộc."
        />
      ) : (
        <>
          <Alert
            type="info"
            showIcon
            title="Thu cọc và chuyển hợp đồng sang Đang hiệu lực"
            description={
              <>
                Mức cọc bắt buộc:{' '}
                <strong>
                  {deposit.required != null
                    ? formatMoney(deposit.required)
                    : 'chưa xác định'}
                </strong>
                {contract.activationDeadline
                  ? ` · Hạn kích hoạt: ${formatDateTime(contract.activationDeadline)}`
                  : ''}
                . Hệ thống ghi giao dịch cọc, chuyển giường giữ chỗ sang đang ở
                trong cùng một giao dịch.
              </>
            }
          />

          <FormGrid>
            <div className="dms-form-span-full">
              <Checkbox
                checked={paperContractSigned}
                onChange={(event) => {
                  setPaperContractSigned(event.target.checked)
                  if (event.target.checked)
                    setErrors((current) => ({
                      ...current,
                      paperContractSigned: undefined,
                    }))
                }}
              >
                Đã ký giấy tờ hợp đồng với sinh viên và đối chiếu thông tin.
              </Checkbox>
              {errors.paperContractSigned && (
                <div role="alert" className="dms-field-error">
                  {errors.paperContractSigned}
                </div>
              )}
            </div>

            <FormField
              label="Số tiền cọc đã thu"
              required
              error={errors.depositAmount}
              hint={
                deposit.required != null
                  ? `Phải bằng mức cọc của tòa: ${formatMoney(deposit.required)}.`
                  : 'Backend chưa trả mức cọc; nhập đúng số tiền đã thu theo cấu hình tòa nhà.'
              }
            >
              {(control) => (
                <InputNumber
                  {...control}
                  min={1}
                  precision={0}
                  style={{ width: '100%' }}
                  value={depositAmount}
                  addonAfter="đ"
                  onChange={(value) => {
                    setDepositAmount(
                      typeof value === 'number' ? value : null,
                    )
                    setErrors((current) => ({
                      ...current,
                      depositAmount: undefined,
                    }))
                  }}
                />
              )}
            </FormField>

            <FormField label="Hình thức thanh toán" required>
              {(control) => (
                <RadioGroup
                  {...control}
                  value={paymentMethod}
                  onChange={(event) =>
                    setPaymentMethod(String(event.target.value))
                  }
                >
                  <Radio value={PaymentMethod.CASH}>Tiền mặt</Radio>
                  <Radio value={PaymentMethod.TRANSFER}>Chuyển khoản</Radio>
                </RadioGroup>
              )}
            </FormField>

            <div className="dms-form-span-full">
              <FormField
                label="Ghi chú"
                error={errors.note}
                hint="Tùy chọn, tối đa 500 ký tự; lưu cùng giao dịch thu cọc."
              >
                {(control) => (
                  <TextArea
                    {...control}
                    rows={3}
                    maxLength={500}
                    showCount
                    placeholder="Ví dụ: sinh viên nộp tiền mặt tại văn phòng Ban quản lý..."
                    value={note}
                    onChange={(event) => setNote(event.target.value)}
                  />
                )}
              </FormField>
            </div>
          </FormGrid>
        </>
      )}
    </Modal>
  )
}
