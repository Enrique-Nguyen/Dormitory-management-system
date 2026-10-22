import { useState } from 'react'
import { Modal, Form, Radio, Select, DatePicker, message, Alert } from 'antd'
import { ThunderboltOutlined, HomeOutlined } from '@ant-design/icons'
import { useIssueRoomFeeInvoices, useIssueUtilityInvoices } from './index'
import { useRoomCatalog } from '../room'
import dayjs from 'dayjs'

interface Props {
  open: boolean
  onClose: () => void
  currentUnitId?: number
}

export default function IssueInvoicesModal({ open, onClose, currentUnitId }: Props) {
  const [issueType, setIssueType] = useState<'ROOM_FEE' | 'UTILITY'>('ROOM_FEE')
  const [form] = Form.useForm()

  const issueRoomFee = useIssueRoomFeeInvoices()
  const issueUtility = useIssueUtilityInvoices()

  // Lấy danh mục học kỳ từ CRUD lookup
  const { data: semesters, isLoading: loadingSemesters } = useRoomCatalog('/semesters' as any)

  const handleSubmit = async () => {
    try {
      const values = await form.validateFields()
      if (issueType === 'ROOM_FEE') {
        const res = await issueRoomFee.mutateAsync({ semesterId: values.semesterId })
        message.success(`Đã phát hành thành công ${res.count} hóa đơn tiền phòng!`)
      } else {
        const billingMonth = dayjs(values.billingMonth).startOf('month').format('YYYY-MM-DD')
        const unitId = currentUnitId || values.managementUnitId || 1
        const res = await issueUtility.mutateAsync({
          managementUnitId: unitId,
          billingMonth,
        })
        message.success(`Đã phát hành thành công ${res.count} hóa đơn điện nước!`)
      }
      form.resetFields()
      onClose()
    } catch {
      // Handled by onError handler
    }
  }

  const isPending = issueRoomFee.isPending || issueUtility.isPending

  return (
    <Modal
      title={
        <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
          {issueType === 'ROOM_FEE' ? (
            <HomeOutlined style={{ color: '#1890ff', fontSize: 20 }} />
          ) : (
            <ThunderboltOutlined style={{ color: '#faad14', fontSize: 20 }} />
          )}
          <span style={{ fontWeight: 600 }}>Phát hành hóa đơn hàng loạt</span>
        </div>
      }
      open={open}
      onCancel={onClose}
      onOk={handleSubmit}
      confirmLoading={isPending}
      okText="Xác nhận phát hành"
      cancelText="Hủy bỏ"
      destroyOnClose
    >
      <div style={{ marginBottom: 16 }}>
        <Radio.Group
          value={issueType}
          onChange={(e) => setIssueType(e.target.value)}
          buttonStyle="solid"
          style={{ width: '100%', display: 'flex' }}
        >
          <Radio.Button value="ROOM_FEE" style={{ flex: 1, textAlign: 'center' }}>
            <HomeOutlined /> Tiền phòng học kỳ
          </Radio.Button>
          <Radio.Button value="UTILITY" style={{ flex: 1, textAlign: 'center' }}>
            <ThunderboltOutlined /> Điện nước tháng
          </Radio.Button>
        </Radio.Group>
      </div>

      <Alert
        message={
          issueType === 'ROOM_FEE'
            ? 'Hệ thống sẽ quét tất cả hợp đồng ACTIVE trong học kỳ được chọn, chốt mức giá snapshot và tạo hóa đơn tiền phòng. Các sinh viên đã có hóa đơn sẽ tự động được bỏ qua (BR-04).'
            : 'Hệ thống sẽ tổng hợp chỉ số điện & nước đã ghi trong tháng cho từng phòng thuộc cơ sở quản lý, áp dụng bảng giá tương ứng và tạo hóa đơn gửi phòng (BR-03, BR-04).'
        }
        type="info"
        showIcon
        style={{ marginBottom: 16, fontSize: 13 }}
      />

      <Form form={form} layout="vertical">
        {issueType === 'ROOM_FEE' ? (
          <Form.Item
            name="semesterId"
            label="Học kỳ áp dụng"
            rules={[{ required: true, message: 'Vui lòng chọn học kỳ' }]}
          >
            <Select
              placeholder="Chọn học kỳ..."
              loading={loadingSemesters}
              options={(semesters ?? []).map((s: any) => ({
                label: `${s.name ?? s.code ?? ''} (${s.academicYear ?? ''})`,
                value: s.id,
              }))}
            />
          </Form.Item>
        ) : (
          <Form.Item
            name="billingMonth"
            label="Tháng chốt điện nước"
            rules={[{ required: true, message: 'Vui lòng chọn tháng' }]}
            initialValue={dayjs().subtract(1, 'month').startOf('month')}
          >
            <DatePicker
              picker="month"
              style={{ width: '100%' }}
              placeholder="Chọn tháng..."
              format="MM/YYYY"
            />
          </Form.Item>
        )}
      </Form>
    </Modal>
  )
}
