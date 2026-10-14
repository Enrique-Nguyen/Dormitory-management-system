import { CalendarOutlined } from '@ant-design/icons'
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
    <Card size="small" className="dms-invoice-card">
      <Flex vertical gap={10}>
        <Typography.Text strong className="dms-wrap" style={{ fontSize: 14 }}>
          {title}
        </Typography.Text>
        <div className="dms-invoice-amount">
          {formatMoney(amount)}
        </div>
        <div style={{ fontSize: 13, color: 'var(--color-text-secondary)', display: 'flex', alignItems: 'center', gap: 6 }}>
          <CalendarOutlined style={{ color: '#9CA3AF' }} />
          <span>Hạn thanh toán: <strong>{formatDate(dueDate)}</strong></span>
        </div>
        {children}
        <div>
          <InvoiceStatus status={status} dueDate={dueDate} />
        </div>
        {onView && (
          <Button onClick={onView} block style={{ marginTop: 4 }}>
            Xem chi tiết
          </Button>
        )}
      </Flex>
    </Card>
  )
}
