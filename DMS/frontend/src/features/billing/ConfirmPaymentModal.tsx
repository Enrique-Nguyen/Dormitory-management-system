import { Modal, Form, Radio, Input, message, Alert } from 'antd'
import { CheckCircleOutlined, DollarOutlined, BankOutlined } from '@ant-design/icons'
import { useConfirmPayment } from './index'

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
    } catch {
      // Handled by onError
    }
  }

  return (
    <Modal
      title={
        <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
          <CheckCircleOutlined style={{ color: '#52c41a', fontSize: 20 }} />
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
      destroyOnClose
    >
      {totalAmount != null && (
        <Alert
          message={`Số tiền cần thu: ${totalAmount.toLocaleString('vi-VN')} đ`}
          type="info"
          showIcon
          style={{ marginBottom: 16, fontWeight: 600 }}
        />
      )}

      <Form form={form} layout="vertical" initialValues={{ paymentMethod: 'TRANSFER' }}>
        <Form.Item
          name="paymentMethod"
          label="Phương thức thanh toán"
          rules={[{ required: true, message: 'Vui lòng chọn phương thức' }]}
        >
          <Radio.Group buttonStyle="solid" style={{ width: '100%', display: 'flex' }}>
            <Radio.Button value="TRANSFER" style={{ flex: 1, textAlign: 'center' }}>
              <BankOutlined /> Chuyển khoản ngân hàng
            </Radio.Button>
            <Radio.Button value="CASH" style={{ flex: 1, textAlign: 'center' }}>
              <DollarOutlined /> Tiền mặt
            </Radio.Button>
          </Radio.Group>
        </Form.Item>

        <Form.Item name="payerName" label="Họ tên người nộp tiền (nếu có)">
          <Input placeholder="Ví dụ: Nguyễn Văn A (Trưởng phòng)..." />
        </Form.Item>
      </Form>
    </Modal>
  )
}
