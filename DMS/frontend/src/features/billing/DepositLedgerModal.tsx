import { useState } from 'react'
import {
  Modal,
  Table,
  Tag,
  Descriptions,
  Button,
  Space,
  Form,
  InputNumber,
  Input,
  message,
  Divider,
  Alert,
  Spin,
} from 'antd'
import {
  SafetyCertificateOutlined,
  PlusCircleOutlined,
  RollbackOutlined,
  HistoryOutlined,
} from '@ant-design/icons'
import {
  useDepositBalance,
  useDepositTransactions,
  useRecordDepositTopup,
  useRecordDepositRefund,
} from './index'
import dayjs from 'dayjs'

interface Props {
  contractId: number
  contractCode?: string
  studentName?: string
  open: boolean
  onClose: () => void
  readOnly?: boolean
}

export default function DepositLedgerModal({
  contractId,
  contractCode,
  studentName,
  open,
  onClose,
  readOnly = false,
}: Props) {
  const { data: balance, isLoading: loadingBalance } = useDepositBalance(contractId)
  const { data: transactions, isLoading: loadingTxs } = useDepositTransactions(contractId)

  const [topupModalOpen, setTopupModalOpen] = useState(false)
  const [refundModalOpen, setRefundModalOpen] = useState(false)
  const [form] = Form.useForm()

  const recordTopup = useRecordDepositTopup(contractId)
  const recordRefund = useRecordDepositRefund(contractId)

  const handleTopup = async () => {
    try {
      const values = await form.validateFields()
      await recordTopup.mutateAsync({
        amount: values.amount,
        note: values.note,
      })
      message.success('Đã ghi nhận nộp bù cọc thành công!')
      form.resetFields()
      setTopupModalOpen(false)
    } catch {
      // Handled by onError
    }
  }

  const handleRefund = async () => {
    try {
      const values = await form.validateFields()
      await recordRefund.mutateAsync({
        amount: values.amount,
        note: values.note,
      })
      message.success('Đã ghi nhận hoàn cọc thành công!')
      form.resetFields()
      setRefundModalOpen(false)
    } catch {
      // Handled by onError
    }
  }

  const columns = [
    {
      title: 'Thời gian',
      dataIndex: 'createdAt',
      key: 'createdAt',
      render: (v: string) => (v ? dayjs(v).format('DD/MM/YYYY HH:mm') : '-'),
      width: 140,
    },
    {
      title: 'Loại GD',
      dataIndex: 'type',
      key: 'type',
      width: 110,
      render: (type: string) => {
        switch (type) {
          case 'DEPOSIT':
            return <Tag color="blue">Nộp ban đầu</Tag>
          case 'DEDUCTION':
            return <Tag color="red">Khấu trừ vi phạm</Tag>
          case 'TOPUP':
            return <Tag color="green">Nộp bổ sung</Tag>
          case 'REFUND':
            return <Tag color="purple">Hoàn trả cọc</Tag>
          default:
            return <Tag>{type}</Tag>
        }
      },
    },
    {
      title: 'Số tiền (VNĐ)',
      dataIndex: 'amount',
      key: 'amount',
      align: 'right' as const,
      render: (amount: number) => {
        const isPositive = amount > 0
        return (
          <span
            style={{
              fontWeight: 600,
              color: isPositive ? '#52c41a' : '#ff4d4f',
            }}
          >
            {isPositive ? `+${amount.toLocaleString('vi-VN')}` : amount.toLocaleString('vi-VN')} đ
          </span>
        )
      },
    },
    {
      title: 'Số dư sau GD',
      dataIndex: 'balanceAfter',
      key: 'balanceAfter',
      align: 'right' as const,
      render: (v: number) => (
        <span style={{ fontWeight: 600 }}>{v.toLocaleString('vi-VN')} đ</span>
      ),
    },
    {
      title: 'Ghi chú / Tham chiếu',
      dataIndex: 'note',
      key: 'note',
      render: (note: string, record: any) => (
        <div>
          <div>{note || '-'}</div>
          {record.refTicketId && (
            <Tag color="orange" style={{ marginTop: 2 }}>
              Ticket #{record.refTicketId}
            </Tag>
          )}
        </div>
      ),
    },
    {
      title: 'Cán bộ ghi nhận',
      dataIndex: 'staffName',
      key: 'staffName',
      render: (v: string) => v || 'Hệ thống',
      width: 130,
    },
  ]

  return (
    <>
      <Modal
        title={
          <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
            <SafetyCertificateOutlined style={{ color: '#52c41a', fontSize: 20 }} />
            <span style={{ fontWeight: 600 }}>
              Sổ tiền cọc hợp đồng {contractCode ? `(${contractCode})` : ''}
            </span>
          </div>
        }
        open={open}
        onCancel={onClose}
        footer={[
          <Button key="close" onClick={onClose}>
            Đóng
          </Button>,
        ]}
        width={850}
        destroyOnClose
      >
        {loadingBalance ? (
          <div style={{ textAlign: 'center', padding: 30 }}>
            <Spin />
          </div>
        ) : balance ? (
          <div>
            <Descriptions bordered size="small" column={{ xs: 1, sm: 2, md: 3 }} style={{ marginBottom: 16 }}>
              <Descriptions.Item label="Sinh viên">{studentName || balance.studentName || '-'}</Descriptions.Item>
              <Descriptions.Item label="Mã sinh viên">{balance.studentCode || '-'}</Descriptions.Item>
              <Descriptions.Item label="Phòng / Tòa">
                {balance.roomCode ? `${balance.roomCode} (${balance.buildingCode ?? ''})` : '-'}
              </Descriptions.Item>
              <Descriptions.Item label="Mức cọc quy định">
                <span style={{ fontWeight: 600 }}>
                  {balance.depositRequired.toLocaleString('vi-VN')} đ
                </span>
              </Descriptions.Item>
              <Descriptions.Item label="Số dư cọc hiện tại">
                <span
                  style={{
                    fontWeight: 700,
                    fontSize: 15,
                    color:
                      balance.balance < balance.depositRequired
                        ? '#ff4d4f'
                        : '#52c41a',
                  }}
                >
                  {balance.balance.toLocaleString('vi-VN')} đ
                </span>
              </Descriptions.Item>
              <Descriptions.Item label="Trạng thái cọc">
                {balance.status === 'TOPUP_NEEDED' ? (
                  <Tag color="error">Cần nộp bù cọc</Tag>
                ) : balance.status === 'REFUNDED' ? (
                  <Tag color="purple">Đã tất toán</Tag>
                ) : (
                  <Tag color="success">Đầy đủ</Tag>
                )}
              </Descriptions.Item>
            </Descriptions>

            {balance.status === 'TOPUP_NEEDED' && (
              <Alert
                message="Cần nộp bù cọc"
                description={`Hợp đồng này bị khấu trừ tiền cọc do đền bù hư hỏng vật tư và hiện đang thiếu ${(
                  balance.depositRequired - balance.balance
                ).toLocaleString('vi-VN')} đ.`}
                type="warning"
                showIcon
                style={{ marginBottom: 16 }}
              />
            )}

            {!readOnly && (
              <div style={{ display: 'flex', gap: 10, marginBottom: 16, justifyContent: 'flex-end' }}>
                <Button
                  type="primary"
                  icon={<PlusCircleOutlined />}
                  onClick={() => {
                    const diff = Math.max(0, balance.depositRequired - balance.balance)
                    form.setFieldsValue({ amount: diff > 0 ? diff : undefined })
                    setTopupModalOpen(true)
                  }}
                >
                  Nộp bù cọc
                </Button>
                {balance.balance > 0 && (
                  <Button
                    danger
                    icon={<RollbackOutlined />}
                    onClick={() => {
                      form.setFieldsValue({ amount: balance.balance })
                      setRefundModalOpen(true)
                    }}
                  >
                    Hoàn trả cọc
                  </Button>
                )}
              </div>
            )}

            <Divider style={{ margin: '12px 0' }}>
              <Space>
                <HistoryOutlined />
                <span>Lịch sử biến động sổ cọc</span>
              </Space>
            </Divider>

            <Table
              dataSource={transactions ?? []}
              columns={columns}
              rowKey="id"
              size="small"
              loading={loadingTxs}
              pagination={{ pageSize: 5 }}
            />
          </div>
        ) : (
          <Alert message="Chưa có dữ liệu sổ cọc cho hợp đồng này." type="info" />
        )}
      </Modal>

      {/* Modal nộp bù cọc */}
      <Modal
        title="Ghi nhận nộp bù cọc"
        open={topupModalOpen}
        onCancel={() => setTopupModalOpen(false)}
        onOk={handleTopup}
        confirmLoading={recordTopup.isPending}
        okText="Xác nhận nộp bù"
        cancelText="Hủy"
        destroyOnClose
      >
        <Form form={form} layout="vertical">
          <Form.Item
            name="amount"
            label="Số tiền nộp bổ sung (VNĐ)"
            rules={[
              { required: true, message: 'Vui lòng nhập số tiền' },
              { type: 'number', min: 1000, message: 'Số tiền tối thiểu 1.000 đ' },
            ]}
          >
            <InputNumber
              style={{ width: '100%' }}
              formatter={(value) => `${value}`.replace(/\B(?=(\d{3})+(?!\d))/g, ',')}
              parser={(value) => value?.replace(/\$\s?|(,*)/g, '') as any}
            />
          </Form.Item>
          <Form.Item name="note" label="Ghi chú">
            <Input.TextArea placeholder="Lý do nộp bổ sung..." rows={3} />
          </Form.Item>
        </Form>
      </Modal>

      {/* Modal hoàn cọc */}
      <Modal
        title="Ghi nhận hoàn trả cọc khi trả phòng"
        open={refundModalOpen}
        onCancel={() => setRefundModalOpen(false)}
        onOk={handleRefund}
        confirmLoading={recordRefund.isPending}
        okText="Xác nhận hoàn cọc"
        cancelText="Hủy"
        destroyOnClose
      >
        <Alert
          message="Lưu ý: Chỉ thực hiện hoàn cọc khi sinh viên làm thủ tục trả phòng hoặc kết thúc hợp đồng."
          type="info"
          showIcon
          style={{ marginBottom: 16 }}
        />
        <Form form={form} layout="vertical">
          <Form.Item
            name="amount"
            label="Số tiền hoàn trả (VNĐ)"
            rules={[
              { required: true, message: 'Vui lòng nhập số tiền' },
              { type: 'number', min: 1000, message: 'Số tiền tối thiểu 1.000 đ' },
            ]}
          >
            <InputNumber
              style={{ width: '100%' }}
              formatter={(value) => `${value}`.replace(/\B(?=(\d{3})+(?!\d))/g, ',')}
              parser={(value) => value?.replace(/\$\s?|(,*)/g, '') as any}
            />
          </Form.Item>
          <Form.Item name="note" label="Ghi chú">
            <Input.TextArea placeholder="Biên bản bàn giao phòng, số tài khoản nhận hoàn cọc..." rows={3} />
          </Form.Item>
        </Form>
      </Modal>
    </>
  )
}
