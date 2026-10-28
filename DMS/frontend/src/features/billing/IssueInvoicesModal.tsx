import { useState } from 'react'
import { App, Form } from 'antd'
import { FileAddOutlined, HomeOutlined, ThunderboltOutlined } from '@ant-design/icons'
import { useQuery } from '@tanstack/react-query'
import dayjs, { type Dayjs } from 'dayjs'
import { Alert, DatePicker, FormGrid, Modal, Radio, RadioGroup, Select } from '../../components'
import { InvoiceType } from '../../constants/statusConfig'
import { Permission } from '../../constants/authorization'
import { hasPermission } from '../../services/api/auth'
import { useMe } from '../account/useMe'
import { getLookupRecords, validationFieldErrors } from '../crud/api'
import { billingUiLabels } from '../crud/resources'
import { formatMoney, formatNumber } from '../../utils/format'
import { useIssueRoomFeeInvoices, useIssueUtilityInvoices } from './useBilling'

interface Props {
  open: boolean
  onClose: () => void
  currentUnitId?: number | null
}
interface IssueForm { semesterId: number; billingMonth: Dayjs }

export default function IssueInvoicesModal({ open, onClose, currentUnitId }: Props) {
  const [issueType, setIssueType] = useState<string>(InvoiceType.ROOM_FEE)
  const { message } = App.useApp()
  const [form] = Form.useForm<IssueForm>()
  const { data: me } = useMe()
  const roomFee = useIssueRoomFeeInvoices()
  const utility = useIssueUtilityInvoices()
  const canIssue = hasPermission(me, Permission.INVOICE_CREATE)
  const { data: semesters = [], isLoading: loadingSemesters, isError: semestersError } = useQuery({
    queryKey: ['crud-lookup', '/semesters'],
    queryFn: () => getLookupRecords('/semesters'),
    enabled: open && issueType === InvoiceType.ROOM_FEE && hasPermission(me, Permission.SEMESTER_READ),
  })
  const pending = roomFee.isPending || utility.isPending
  const semesterHint = semestersError
    ? billingUiLabels.issueSemesterLoadError
    : !loadingSemesters && semesters.length === 0
      ? billingUiLabels.issueNoSemesters
      : undefined
  const handleSubmit = async () => {
    if (!canIssue || pending) return
    try {
      const values = await form.validateFields()
      const result = issueType === InvoiceType.ROOM_FEE
        ? await roomFee.mutateAsync({ semesterId: values.semesterId })
        : await utility.mutateAsync({
            managementUnitId: currentUnitId ?? undefined,
            billingMonth: values.billingMonth.startOf('month').format('YYYY-MM-DD'),
          })
      message.success(
        'Đã tạo ' + formatNumber(result.totalCreated) + ' hóa đơn, bỏ qua '
        + formatNumber(result.totalSkipped) + '; tổng tiền '
        + formatMoney(result.totalAmount) + '.',
      )
      form.resetFields()
      onClose()
    } catch (error) {
      const fields = validationFieldErrors(error)
      if (fields.length) form.setFields(fields.filter((item): item is { field: keyof IssueForm; message: string } => item.field === 'semesterId' || item.field === 'billingMonth')
        .map(({ field, message }) => ({ name: field, errors: [message] })))
    }
  }
  return (
    <Modal
      title={
        <span className="dms-modal-title">
          <FileAddOutlined className="dms-modal-title-icon" aria-hidden="true" />
          {billingUiLabels.issueTitle}
        </span>
      }
      open={open}
      onCancel={() => { if (!pending) onClose() }}
      closable={!pending}
      mask={{ closable: !pending }}
      cancelButtonProps={{ disabled: pending }}
      onOk={handleSubmit}
      confirmLoading={pending}
      okButtonProps={{ disabled: !canIssue }}
      okText={billingUiLabels.issueSubmit}
      cancelText={billingUiLabels.cancel}
      destroyOnHidden
    >
      <p className="dms-modal-lead">{billingUiLabels.issueLead}</p>
      <div className="dms-modal-section">
        <span id="issue-type-label" className="dms-modal-section-label">
          {billingUiLabels.issueTypeLabel}
        </span>
        <RadioGroup
          aria-labelledby="issue-type-label"
          className="dms-choice-grid"
          value={issueType}
          disabled={pending}
          onChange={(event) => {
            setIssueType(event.target.value)
            form.resetFields()
          }}
        >
          <Radio value={InvoiceType.ROOM_FEE} className="dms-choice">
            <span className="dms-choice-title">
              <HomeOutlined aria-hidden="true" />
              {billingUiLabels.issueRoomFee}
            </span>
            <span className="dms-choice-hint">{billingUiLabels.issueRoomFeeHint}</span>
          </Radio>
          <Radio value={InvoiceType.UTILITY} className="dms-choice">
            <span className="dms-choice-title">
              <ThunderboltOutlined aria-hidden="true" />
              {billingUiLabels.issueUtility}
            </span>
            <span className="dms-choice-hint">{billingUiLabels.issueUtilityHint}</span>
          </Radio>
        </RadioGroup>
      </div>
      <Form form={form} layout="vertical" disabled={pending} initialValues={{ billingMonth: dayjs().subtract(1, 'month').startOf('month') }}>
        <FormGrid className="dms-form-grid-single">
          {issueType === InvoiceType.ROOM_FEE ? (
            <Form.Item
              name="semesterId"
              label={billingUiLabels.issueSemester}
              extra={semesterHint}
              rules={[{ required: true, message: 'Vui lòng chọn học kỳ' }]}
            >
              <Select
                loading={loadingSemesters}
                placeholder={billingUiLabels.issueChooseSemester}
                aria-label={billingUiLabels.issueSemester}
                options={semesters.map((semester) => ({ value: Number(semester.id), label: String(semester.name) }))}
              />
            </Form.Item>
          ) : (
            <Form.Item
              name="billingMonth"
              label={billingUiLabels.issueMonth}
              rules={[{ required: true, message: 'Vui lòng chọn tháng' }]}
            >
              <DatePicker
                picker="month"
                format="MM/YYYY"
                placeholder={billingUiLabels.issueChooseMonth}
                style={{ width: '100%' }}
              />
            </Form.Item>
          )}
        </FormGrid>
      </Form>
      <Alert type="info" showIcon title={billingUiLabels.issueNote} style={{ marginTop: 4 }} />
    </Modal>
  )
}
