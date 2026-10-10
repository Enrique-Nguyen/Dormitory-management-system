import { Flex } from 'antd'
import { daysLeft } from '../../utils/format'
import DerivedChip from '../ui/DerivedChip'
import StatusBadge from '../ui/StatusBadge'

interface Props {
  status: string
  dueDate?: string | null
}

/** Chưa thanh toán + qua hạn đóng → thêm chip "Quá hạn" cạnh badge (không lưu trong CSDL). */
export default function InvoiceStatus({ status, dueDate }: Props) {
  const left = status === 'UNPAID' ? daysLeft(dueDate) : null
  return (
    <Flex wrap gap={8} align="center">
      <StatusBadge entity="invoice" status={status} />
      {left !== null && left < 0 && <DerivedChip label="INVOICE_OVERDUE" />}
    </Flex>
  )
}
