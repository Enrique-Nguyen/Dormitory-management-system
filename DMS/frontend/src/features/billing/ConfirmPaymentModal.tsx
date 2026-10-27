import { Form, message } from 'antd'
import { BankOutlined, CheckCircleOutlined, DollarOutlined } from '@ant-design/icons'
import { Alert, Input, Modal, Radio, RadioGroup } from '../../components'
import { PaymentMethod } from '../../constants/statusConfig'
import { validationFieldErrors } from '../crud/api'
import { billingUiLabels } from '../crud/resources'
import { formatMoney } from '../../utils/format'
import { useConfirmPayment } from './useBilling'

interface Props {
  invoiceId: number
  invoiceCode?: string
  totalAmount?: number
  open: boolean
  onClose: () => void
  onSuccess?: () => void
}

export default function ConfirmPaymentModal({
  invoiceId,
  invoiceCode,
  totalAmount,
  open,
  onClose,
  onSuccess,
}: Props) {
  const [form] = Form.useForm()
  const confirmPayment = useConfirmPayment(invoiceId)

  const handleSubmit = async () => {
    if (confirmPayment.isPending) return
    try {
      const values = await form.validateFields()
      await confirmPayment.mutateAsync({
        paymentMethod: values.paymentMethod,
        payerName: String(values.payerName).trim(),
      })
      message.success(
        totalAmount != null
          ? `Đã xác nhận thu ${formatMoney(totalAmount)} cho hóa đơn ${invoiceCode ?? ''}.`
          : `Đã xác nhận thanh toán hóa đơn ${invoiceCode ?? ''}.`,
      )
      form.resetFields()
      onClose()
      onSuccess?.()
    } catch (error) {
      form.setFields(validationFieldErrors(error).map(({ field, message }) => ({ name: field, errors: [message] })))
    }
  }

  return (
    <Modal
      title={
        <span className="dms-modal-title">
          <CheckCircleOutlined className="dms-modal-title-icon" aria-hidden="true" />
          {billingUiLabels.paymentTitle}
        </span>
      }
      open={open}
      onCancel={() => { if (!confirmPayment.isPending) onClose() }}
      closable={!confirmPayment.isPending}
      mask={{ closable: !confirmPayment.isPending }}
      cancelButtonProps={{ disabled: confirmPayment.isPending }}
      onOk={handleSubmit}
      confirmLoading={confirmPayment.isPending}
      okText={billingUiLabels.confirmReceived}
      cancelText={billingUiLabels.cancel}
      destroyOnHidden
    >
      <div className="dms-modal-summary">
        <div>
          <span className="dms-modal-summary-label">{billingUiLabels.amountDue}</span>
          <strong className="dms-modal-summary-value">
            {totalAmount != null ? formatMoney(totalAmount) : '—'}
          </strong>
        </div>
        {!!invoiceCode && (
          <span className="dms-modal-summary-code">
            {billingUiLabels.invoiceCode}: <strong>{invoiceCode}</strong>
          </span>
        )}
      </div>

      <Form form={form} layout="vertical" disabled={confirmPayment.isPending} initialValues={{ paymentMethod: PaymentMethod.TRANSFER }}>
        <Form.Item
          name="paymentMethod"
          label={billingUiLabels.paymentMethod}
          rules={[{ required: true, message: 'Vui lòng chọn phương thức thanh toán' }]}
        >
          <RadioGroup className="dms-choice-grid">
            <Radio value={PaymentMethod.TRANSFER} className="dms-choice">
              <span className="dms-choice-title">
                <BankOutlined aria-hidden="true" />
                {billingUiLabels.transfer}
              </span>
              <span className="dms-choice-hint">{billingUiLabels.transferHint}</span>
            </Radio>
            <Radio value={PaymentMethod.CASH} className="dms-choice">
              <span className="dms-choice-title">
                <DollarOutlined aria-hidden="true" />
                {billingUiLabels.cash}
              </span>
              <span className="dms-choice-hint">{billingUiLabels.cashHint}</span>
            </Radio>
          </RadioGroup>
        </Form.Item>

        <Form.Item
          name="payerName"
          label={billingUiLabels.payerName}
          extra={billingUiLabels.payerHint}
          rules={[
            { required: true, whitespace: true, message: 'Vui lòng nhập người nộp tiền' },
            { max: 100, message: 'Tối đa 100 ký tự' },
          ]}
        >
          <Input placeholder={billingUiLabels.payerPlaceholder} />
        </Form.Item>
      </Form>

      <Alert type="info" showIcon title={billingUiLabels.paymentNote} />
    </Modal>
  )
}
