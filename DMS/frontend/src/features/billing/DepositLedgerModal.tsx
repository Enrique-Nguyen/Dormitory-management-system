import { useState } from 'react'
import { Form, message, Spin, type TableColumnsType } from 'antd'
import { Alert, Button, DataTable, Descriptions, FormGrid, InputNumber, Modal, StatusBadge, TextArea } from '../../components'
import { BillingAction, depositTransactionLabels } from '../../constants/statusConfig'
import { Permission } from '../../constants/authorization'
import { hasPermission } from '../../services/api/auth'
import type { DepositTransaction } from '../../services/api/billing'
import { useMe } from '../account/useMe'
import { validationFieldErrors } from '../crud/api'
import { useDepositBalance, useDepositTransactions, useRecordDepositTopup, useRecordDepositRefund } from './useBilling'
import { formatDateTime, formatMoney, formatDate } from '../../utils/format'

interface Props {
  contractId: number
  contractCode?: string
  studentName?: string
  open: boolean
  onClose: () => void
  readOnly?: boolean
}
interface LedgerForm { amount: number; note?: string }

export default function DepositLedgerModal({ contractId, contractCode, studentName, open, onClose, readOnly = false }: Props) {
  const { data: me } = useMe()
  const { data: balance, isLoading: loadingBalance, error: balanceError } = useDepositBalance(open ? contractId : undefined)
  const { data: transactions, isLoading: loadingTxs, error: transactionError } = useDepositTransactions(open ? contractId : undefined)
  const [action, setAction] = useState<'topup' | 'refund' | null>(null)
  const [form] = Form.useForm<LedgerForm>()
  const topup = useRecordDepositTopup(contractId)
  const refund = useRecordDepositRefund(contractId)
  const canWrite = !readOnly && hasPermission(me, Permission.DEPOSIT_CREATE)
  const canTopup = canWrite && balance?.allowedActions.includes(BillingAction.DEPOSIT_TOPUP)
  const canRefund = canWrite && balance?.allowedActions.includes(BillingAction.DEPOSIT_REFUND)
  const maxAmount = action === 'topup' ? balance?.topupNeeded : balance?.balance
  const save = async () => {
    try {
      const values = await form.validateFields()
      if (action === 'topup') await topup.mutateAsync(values)
      else if (action === 'refund') await refund.mutateAsync(values)
      else return
      message.success('Đã ghi nhận giao dịch tiền cọc.')
      setAction(null)
    } catch (error) {
      form.setFields(validationFieldErrors(error).filter((item): item is { field: keyof LedgerForm; message: string } => item.field === 'amount' || item.field === 'note').map(({ field, message }) => ({ name: field, errors: [message] })))
    }
  }
  const columns: TableColumnsType<DepositTransaction> = [
    { title: 'Thời gian', dataIndex: 'createdAt', render: (value: string) => formatDateTime(value) },
    { title: 'Giao dịch', dataIndex: 'type', render: (value: string) => depositTransactionLabels[value] ?? value },
    { title: 'Số tiền', dataIndex: 'amount', align: 'right', render: (value: number) => formatMoney(value) },
    { title: 'Ticket', dataIndex: 'maintenanceRequestId', render: (value: number | null) => value ?? '—' },
    { title: 'Hạn nộp bù', dataIndex: 'dueDate', render: (value: string | null) => formatDate(value) },
    { title: 'Người xác nhận (ID)', dataIndex: 'confirmedBy', render: (value: number | null) => value ?? '—' },
    { title: 'Ghi chú', dataIndex: 'note' },
  ]
  return <>
    <Modal title={'Sổ cọc — ' + (contractCode ?? balance?.contractCode ?? contractId)}
      open={open} onCancel={onClose} footer={<Button onClick={onClose}>Đóng</Button>} width={1000} destroyOnHidden>
      {loadingBalance ? <Spin /> : balanceError ? <Alert type="error" title="Không thể tải số dư cọc" /> : balance ? <>
        <Descriptions bordered column={{ xs: 1, sm: 2 }} items={[
          { key: 'student', label: 'Sinh viên', children: studentName ?? balance.studentName },
          { key: 'code', label: 'Mã sinh viên', children: balance.studentCode ?? balance.studentId },
          { key: 'status', label: 'Hợp đồng', children: <StatusBadge entity="contract" status={balance.status} /> },
          { key: 'required', label: 'Mức cọc', children: formatMoney(balance.depositRequired) },
          { key: 'balance', label: 'Số dư', children: formatMoney(balance.balance) },
          { key: 'topup', label: 'Cần bù', children: formatMoney(balance.topupNeeded) },
          { key: 'debt', label: 'Công nợ sau trả phòng', children: formatMoney(balance.depositDebt) },
        ]} />
        {balance.topupOverdue && <Alert style={{ marginBlock: 16 }} type="warning" showIcon title="Khoản bù cọc đã quá hạn" />}
        <div style={{ display: 'flex', gap: 12, marginBlock: 16 }}>
          {canTopup && <Button type="primary" onClick={() => setAction('topup')}>Nộp bù cọc</Button>}
          {canRefund && <Button danger onClick={() => setAction('refund')}>Hoàn cọc</Button>}
        </div>
        {transactionError ? <Alert type="error" title="Không thể tải lịch sử giao dịch" /> : <DataTable<DepositTransaction>
          dataSource={transactions ?? []} columns={columns} rowKey="id" loading={loadingTxs} pagination={false} scroll={{ x: 800 }} />}
      </> : <Alert type="info" title="Chưa có dữ liệu sổ cọc" />}
    </Modal>
    <Modal title={action === 'topup' ? 'Ghi nhận nộp bù cọc' : 'Ghi nhận hoàn cọc'} open={action !== null}
      onCancel={() => setAction(null)} onOk={save} confirmLoading={topup.isPending || refund.isPending}
      destroyOnHidden okText="Xác nhận" cancelText="Hủy"
      afterOpenChange={(visible) => { if (visible) { form.resetFields(); form.setFieldsValue({ amount: action === 'topup' ? balance?.topupNeeded : balance?.balance }) } }}>
      <Form form={form} layout="vertical"><FormGrid>
        <Form.Item
          name="amount"
          label="Số tiền (đ)"
          extra={
            maxAmount
              ? `Tối đa ${formatMoney(maxAmount)}${action === 'topup' ? ' (số cọc còn thiếu)' : ' (số dư hiện tại)'}`
              : undefined
          }
          rules={[{ required: true, message: 'Vui lòng nhập số tiền' }, { type: 'number', min: 1, message: 'Số tiền phải lớn hơn 0' }]}
        >
          <InputNumber precision={0} min={1} max={maxAmount} style={{ width: '100%' }} />
        </Form.Item>
        <Form.Item name="note" label="Ghi chú" className="dms-form-span-full" rules={[{ max: 500 }]}>
          <TextArea rows={3} />
        </Form.Item>
      </FormGrid></Form>
    </Modal>
  </>
}
