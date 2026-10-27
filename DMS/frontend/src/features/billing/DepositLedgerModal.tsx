import { useState } from 'react'
import { Form, message, type TableColumnsType } from 'antd'
import { PlusOutlined, ReloadOutlined, RollbackOutlined } from '@ant-design/icons'
import {
  Alert,
  Button,
  DataTable,
  EmptyState,
  FormGrid,
  InputNumber,
  Modal,
  Skeleton,
  StatusBadge,
  TextArea,
} from '../../components'
import { BillingAction, depositTransactionLabels } from '../../constants/statusConfig'
import { Permission } from '../../constants/authorization'
import { hasPermission } from '../../services/api/auth'
import type { DepositTransaction } from '../../services/api/billing'
import { useMe } from '../account/useMe'
import { validationFieldErrors } from '../crud/api'
import { billingUiLabels } from '../crud/resources'
import { useDepositBalance, useDepositTransactions, useRecordDepositTopup, useRecordDepositRefund } from './useBilling'
import { formatDate, formatDateTime, formatMoney, formatSignedMoney } from '../../utils/format'

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
  const { data: balance, isLoading: loadingBalance, error: balanceError, refetch: refetchBalance } = useDepositBalance(open ? contractId : undefined)
  const { data: transactions, isLoading: loadingTxs, error: transactionError, refetch: refetchTransactions } = useDepositTransactions(open ? contractId : undefined)
  const [action, setAction] = useState<'topup' | 'refund' | null>(null)
  const [form] = Form.useForm<LedgerForm>()
  const topup = useRecordDepositTopup(contractId)
  const refund = useRecordDepositRefund(contractId)
  const pending = topup.isPending || refund.isPending
  const canWrite = !readOnly && hasPermission(me, Permission.DEPOSIT_CREATE)
  const canTopup = canWrite && balance?.allowedActions?.includes(BillingAction.DEPOSIT_TOPUP)
  const canRefund = canWrite && balance?.allowedActions?.includes(BillingAction.DEPOSIT_REFUND)
  const save = async () => {
    if (!action || pending) return
    try {
      const values = await form.validateFields()
      const payload = {
        amount: values.amount,
        note: values.note?.trim() || undefined,
      }
      if (action === 'topup') await topup.mutateAsync(payload)
      else await refund.mutateAsync(payload)
      message.success(billingUiLabels.depositSaved)
      setAction(null)
    } catch (error) {
      form.setFields(validationFieldErrors(error).filter((item): item is { field: keyof LedgerForm; message: string } => item.field === 'amount' || item.field === 'note').map(({ field, message }) => ({ name: field, errors: [message] })))
    }
  }
  const columns: TableColumnsType<DepositTransaction> = [
    {
      title: billingUiLabels.depositTime,
      dataIndex: 'createdAt',
      render: (value: string) => formatDateTime(value),
    },
    {
      title: billingUiLabels.depositTransaction,
      dataIndex: 'type',
      render: (value: string) => depositTransactionLabels[value] ?? value,
    },
    {
      title: billingUiLabels.depositTransactionAmount,
      dataIndex: 'amount',
      align: 'right',
      className: 'dms-table-col-number',
      render: (value: number) => (
        <span className={value > 0 ? 'dms-amount-credit' : 'dms-amount-debit'}>
          {formatSignedMoney(value)}
        </span>
      ),
    },
    {
      title: billingUiLabels.depositTicket,
      dataIndex: 'maintenanceRequestId',
      responsive: ['lg'],
      render: (value: number | null) => value ?? '—',
    },
    {
      title: billingUiLabels.depositTransactionDue,
      dataIndex: 'dueDate',
      responsive: ['md'],
      render: (value: string | null) => formatDate(value),
    },
    {
      title: billingUiLabels.depositConfirmedBy,
      dataIndex: 'confirmedBy',
      responsive: ['xl'],
      render: (value: number | null) => value ?? '—',
    },
    {
      title: billingUiLabels.depositTransactionNote,
      dataIndex: 'note',
      responsive: ['sm'],
      render: (value: string | null) => value ?? '—',
    },
  ]
  const metrics = balance
    ? [
        { label: billingUiLabels.depositRequired, value: balance.depositRequired, tone: '' },
        { label: billingUiLabels.depositBalance, value: balance.balance, tone: 'primary' },
        {
          label: billingUiLabels.depositTopupNeeded,
          value: balance.topupNeeded,
          tone: balance.topupOverdue ? 'danger' : balance.topupNeeded > 0 ? 'warning' : '',
        },
        {
          label: billingUiLabels.depositDebt,
          value: balance.depositDebt,
          tone: balance.depositDebt > 0 ? 'danger' : '',
        },
      ]
    : []
  return <>
    <Modal
      title={billingUiLabels.depositTitle + ' · ' + (contractCode ?? balance?.contractCode ?? contractId)}
      open={open}
      onCancel={onClose}
      footer={<Button onClick={onClose}>{billingUiLabels.close}</Button>}
      width={1000}
      destroyOnHidden
    >
      {loadingBalance ? (
        <div className="dms-deposit-loading">
          <Skeleton active paragraph={{ rows: 5 }} className="dms-skeleton" />
        </div>
      ) : balanceError ? (
        <Alert
          type="error"
          showIcon
          title={billingUiLabels.depositLoadError}
          action={
            <Button size="small" icon={<ReloadOutlined />} onClick={() => { void refetchBalance() }}>
              {billingUiLabels.retry}
            </Button>
          }
        />
      ) : balance ? (
        <>
          <div className="dms-deposit-head">
            <div className="dms-deposit-person">
              <span className="dms-deposit-name">{studentName ?? balance.studentName}</span>
              <span className="dms-deposit-code">
                {billingUiLabels.depositStudentCode}: {balance.studentCode ?? balance.studentId}
                {' · '}
                {billingUiLabels.depositContract}: {balance.contractCode}
              </span>
            </div>
            <StatusBadge entity="contract" status={balance.status} />
          </div>

          <div className="dms-deposit-summary" aria-label={billingUiLabels.depositSummary}>
            {metrics.map((metric) => (
              <div
                key={metric.label}
                className={'dms-deposit-metric' + (metric.tone ? ` dms-deposit-metric--${metric.tone}` : '')}
              >
                <span className="dms-deposit-metric-label">{metric.label}</span>
                <span className="dms-deposit-metric-value">{metric.value == null ? '—' : formatMoney(metric.value)}</span>
              </div>
            ))}
          </div>

          {balance.topupOverdue && (
            <Alert
              style={{ marginTop: 16 }}
              type="warning"
              showIcon
              title={billingUiLabels.depositTopupOverdue}
              description={
                balance.oldestUnpaidDue
                  ? `${billingUiLabels.depositTopupDue}: ${formatDate(balance.oldestUnpaidDue)}`
                  : undefined
              }
            />
          )}

          {(canTopup || canRefund) && (
            <div className="dms-deposit-actions">
              {canTopup && (
                <Button type="primary" icon={<PlusOutlined />} onClick={() => setAction('topup')}>
                  {billingUiLabels.depositTopup}
                </Button>
              )}
              {canRefund && (
                <Button danger icon={<RollbackOutlined />} onClick={() => setAction('refund')}>
                  {billingUiLabels.depositRefund}
                </Button>
              )}
            </div>
          )}

          <div className="dms-deposit-section-head">
            <h4 className="dms-deposit-section-title">{billingUiLabels.depositHistory}</h4>
            <span className="dms-dashboard-chart-note">{billingUiLabels.depositHistoryHint}</span>
          </div>
          {transactionError ? (
            <Alert
              type="error"
              showIcon
              title={billingUiLabels.depositTransactionError}
              action={
                <Button size="small" icon={<ReloadOutlined />} onClick={() => { void refetchTransactions() }}>
                  {billingUiLabels.retry}
                </Button>
              }
            />
          ) : (
            <DataTable<DepositTransaction>
              dataSource={transactions ?? []}
              columns={columns}
              rowKey="id"
              loading={loadingTxs}
              pagination={false}
              scroll={{ x: 760 }}
              locale={{
                emptyText: (
                  <EmptyState
                    title={billingUiLabels.depositEmptyHistory}
                    description={billingUiLabels.depositEmptyHistoryHint}
                  />
                ),
              }}
            />
          )}
        </>
      ) : (
        <EmptyState title={billingUiLabels.depositNoData} />
      )}
    </Modal>

    <Modal
      title={action === 'topup' ? billingUiLabels.depositTopupTitle : billingUiLabels.depositRefundTitle}
      open={action !== null}
      onCancel={() => { if (!pending) setAction(null) }}
      onOk={save}
      confirmLoading={pending}
      closable={!pending}
      mask={{ closable: !pending }}
      cancelButtonProps={{ disabled: pending }}
      destroyOnHidden
      okText={billingUiLabels.depositConfirm}
      cancelText={billingUiLabels.cancel}
      afterOpenChange={(visible) => {
        if (visible) {
          form.resetFields()
          form.setFieldsValue({ amount: action === 'topup' ? balance?.topupNeeded : balance?.balance })
        }
      }}
    >
      {balance && (
        <div className="dms-modal-summary">
          <div>
            <span className="dms-modal-summary-label">
              {action === 'topup' ? billingUiLabels.depositTopupNeeded : billingUiLabels.depositBalance}
            </span>
            <strong className="dms-modal-summary-value">
              {formatMoney(action === 'topup' ? balance.topupNeeded : balance.balance)}
            </strong>
          </div>
          <span className="dms-modal-summary-code">
            {billingUiLabels.depositContract}: <strong>{balance.contractCode}</strong>
          </span>
        </div>
      )}
      <Form form={form} layout="vertical" disabled={pending}>
        <FormGrid className="dms-form-grid-single">
          <Form.Item
            name="amount"
            label={billingUiLabels.depositAmount}
            extra={action === 'topup' ? billingUiLabels.depositTopupHelp : billingUiLabels.depositRefundHelp}
            rules={[
              { required: true, message: 'Vui lòng nhập số tiền' },
              { type: 'number', min: 1, message: 'Số tiền phải lớn hơn 0' },
            ]}
          >
            <InputNumber
              precision={0}
              min={1}
              max={action === 'topup' ? balance?.topupNeeded : undefined}
              disabled={action === 'refund'}
              style={{ width: '100%' }}
            />
          </Form.Item>
          <Form.Item name="note" label={billingUiLabels.note} rules={[{ max: 500, message: 'Tối đa 500 ký tự' }]}>
            <TextArea rows={3} />
          </Form.Item>
        </FormGrid>
      </Form>
    </Modal>
  </>
}
