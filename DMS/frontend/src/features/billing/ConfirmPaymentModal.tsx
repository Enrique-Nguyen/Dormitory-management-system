import { Form, message } from 'antd'
import { Modal, Radio, RadioGroup, Input, Alert, FormGrid } from '../../components'
import { PaymentMethod } from '../../constants/statusConfig'
import { validationFieldErrors } from '../crud/api'
import { CheckCircleOutlined, DollarOutlined, BankOutlined } from '@ant-design/icons'
import { useConfirmPayment } from './useBilling'

interface Props {
  invoiceId: number
  invoiceCode?: string
  totalAmount?: number
  open: boolean
  onClose: () => void
}

export default function ConfirmPaymentModal({
  invoiceId,
  invoiceCode,
  totalAmount,
  open,
  onClose,
}: Props) {
  const [form] = Form.useForm()
  const confirmPayment = useConfirmPayment(invoiceId)

  const handleSubmit = async () => {
    try {
      const values = await form.validateFields()
      await confirmPayment.mutateAsync({
        paymentMethod: values.paymentMethod,
        payerName: values.payerName,
      })
      message.success(`Đã xác nhận thu tiền hóa đơn ${invoiceCode ?? ''} thành công!`)
      form.resetFields()
      onClose()
    } catch (error) {
      form.setFields(validationFieldErrors(error).map(({ field, message }) => ({ name: field, errors: [message] })))
    }
  }

  return (
    <Modal
      title={
        <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
          <CheckCircleOutlined style={{ color: 'var(--color-success)', fontSize: 20 }} />
          <span style={{ fontWeight: 600 }}>
            Xác nhận thanh toán {invoiceCode ? `(${invoiceCode})` : ''}
          </span>
        </div>
      }
      open={open}
      onCancel={onClose}
      onOk={handleSubmit}
      confirmLoading={confirmPayment.isPending}
      okText="Xác nhận đã thu"
      cancelText="Hủy bỏ"
      destroyOnHidden
    >
      {totalAmount != null && (
        <Alert
          message={`Số tiền cần thu: ${totalAmount.toLocaleString('vi-VN')} đ`}
          type="info"
          showIcon
          style={{ marginBottom: 16, fontWeight: 600 }}
        />
      )}

      <Form form={form} layout="vertical" initialValues={{ paymentMethod: PaymentMethod.TRANSFER }}>
        <FormGrid>
        <Form.Item
          name="paymentMethod"
          label="Phương thức thanh toán"
          rules={[{ required: true, message: 'Vui lòng chọn phương thức' }]}
        >
          <RadioGroup buttonStyle="solid" style={{ width: '100%', display: 'flex' }}>
            <Radio value={PaymentMethod.TRANSFER} style={{ flex: 1, textAlign: 'center' }}>
              <BankOutlined /> Chuyển khoản ngân hàng
            </Radio>
            <Radio value={PaymentMethod.CASH} style={{ flex: 1, textAlign: 'center' }}>
              <DollarOutlined /> Tiền mặt
            </Radio>
          </RadioGroup>
        </Form.Item>

        <Form.Item name="payerName" label="Họ tên người nộp tiền" rules={[{ required: true, whitespace: true, message: 'Vui lòng nhập người nộp tiền' }, { max: 100 }]}>
          <Input placeholder="Ví dụ: Nguyễn Văn A (Trưởng phòng)..." />
        </Form.Item>
      </FormGrid>
      </Form>
    </Modal>
  )
}
