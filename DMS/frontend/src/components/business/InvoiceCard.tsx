import { Button, Card, Flex, Typography } from 'antd'
import type { ReactNode } from 'react'
import { formatDate, formatMoney } from '../../utils/format'
import InvoiceStatus from './InvoiceStatus'

interface Props {
  /** "Phí phòng · HK1 2026-2027" hoặc "Điện nước phòng K1-A101 · 10/2026" */
  title: string
  amount: number
  dueDate: string
  status: string
  /** Dòng phụ: người nộp, ghi chú "hóa đơn của phòng"... */
  children?: ReactNode
  onView?: () => void
}

/** Thẻ hóa đơn cho sinh viên (UI mục 10). */
export default function InvoiceCard({ title, amount, dueDate, status, children, onView }: Props) {
  return (
    <Card size="small">
      <Flex vertical gap={8}>
        <Typography.Text strong className="dms-wrap">
          {title}
        </Typography.Text>
        <Typography.Title level={3} style={{ margin: 0 }}>
          {formatMoney(amount)}
        </Typography.Title>
        <div>Hạn thanh toán: {formatDate(dueDate)}</div>
        {children}
        <InvoiceStatus status={status} dueDate={dueDate} />
        {onView && (
          <Button onClick={onView} block>
            Xem chi tiết
          </Button>
        )}
      </Flex>
    </Card>
  )
}
