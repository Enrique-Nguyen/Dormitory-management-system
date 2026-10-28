import { useState } from 'react'
import { App, Form } from 'antd'
import { useQuery } from '@tanstack/react-query'
import dayjs, { type Dayjs } from 'dayjs'
import { Alert, DatePicker, FormGrid, Modal, Radio, RadioGroup, Select } from '../../components'
import { InvoiceType } from '../../constants/statusConfig'
import { Permission } from '../../constants/authorization'
import { hasPermission } from '../../services/api/auth'
import { useMe } from '../account/useMe'
import { getLookupRecords, validationFieldErrors } from '../crud/api'
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
  const { data: semesters = [], isLoading: loadingSemesters } = useQuery({
    queryKey: ['crud-lookup', '/semesters'],
    queryFn: () => getLookupRecords('/semesters'),
    enabled: open && issueType === InvoiceType.ROOM_FEE && hasPermission(me, Permission.SEMESTER_READ),
  })
  const handleSubmit = async () => {
    try {
      const values = await form.validateFields()
      const result = issueType === InvoiceType.ROOM_FEE
        ? await roomFee.mutateAsync({ semesterId: values.semesterId })
        : await utility.mutateAsync({
            managementUnitId: currentUnitId ?? undefined,
            billingMonth: values.billingMonth.startOf('month').format('YYYY-MM-DD'),
          })
      message.success('Đã tạo ' + result.totalCreated + ' hóa đơn, bỏ qua ' + result.totalSkipped
        + '; tổng tiền ' + result.totalAmount.toLocaleString('vi-VN') + ' đ.')
      form.resetFields()
      onClose()
    } catch (error) {
      const fields = validationFieldErrors(error)
      if (fields.length) form.setFields(fields.filter((item): item is { field: keyof IssueForm; message: string } => item.field === 'semesterId' || item.field === 'billingMonth')
        .map(({ field, message }) => ({ name: field, errors: [message] })))
    }
  }
  return (
    <Modal title="Phát hành hóa đơn hàng loạt" open={open} onCancel={onClose}
      onOk={handleSubmit} confirmLoading={roomFee.isPending || utility.isPending}
      okButtonProps={{ disabled: !canIssue }} okText="Xác nhận phát hành" cancelText="Hủy" destroyOnHidden>
      <RadioGroup value={issueType} onChange={(event) => setIssueType(event.target.value)}>
        <Radio value={InvoiceType.ROOM_FEE}>Tiền phòng học kỳ</Radio>
        <Radio value={InvoiceType.UTILITY}>Điện nước tháng</Radio>
      </RadioGroup>
      <Alert type="info" showIcon style={{ marginBlock: 16 }}
        title={issueType === InvoiceType.ROOM_FEE
          ? 'Phát hành trong học kỳ hiện tại theo giá trên phân phòng đang ở. Hóa đơn đã có được bỏ qua.'
          : 'Phát hành cho phòng đang có người ở trong phạm vi quản lý. Phòng thiếu chỉ số điện hoặc nước được bỏ qua.'} />
      <Form form={form} layout="vertical" initialValues={{ billingMonth: dayjs().subtract(1, 'month').startOf('month') }}>
        <FormGrid>
          {issueType === InvoiceType.ROOM_FEE ? (
            <Form.Item name="semesterId" label="Học kỳ" rules={[{ required: true, message: 'Vui lòng chọn học kỳ' }]}>
              <Select loading={loadingSemesters} placeholder="Chọn học kỳ"
                options={semesters.map((semester) => ({ value: Number(semester.id), label: String(semester.name) }))} />
            </Form.Item>
          ) : (
            <Form.Item name="billingMonth" label="Tháng điện nước" rules={[{ required: true, message: 'Vui lòng chọn tháng' }]}>
              <DatePicker picker="month" format="MM/YYYY" style={{ width: '100%' }} />
            </Form.Item>
          )}
        </FormGrid>
      </Form>
    </Modal>
  )
}
