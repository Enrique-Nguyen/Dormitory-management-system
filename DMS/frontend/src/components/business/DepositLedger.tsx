import { Flex, Typography } from 'antd'
import { formatDate, formatMoney, formatSignedMoney } from '../../utils/format'

export interface LedgerEntry {
  id: number | string
  date: string
  /** Có dấu: thu/bù/hoàn dương, trừ âm. */
  amount: number
  description: string
}

interface Props {
  entries: LedgerEntry[]
  balance: number
}

/** Sổ cọc dạng ledger (UI mục 9, 20). Dấu +/− đi cùng số tiền, không chỉ dựa vào màu. */
export default function DepositLedger({ entries, balance }: Props) {
  return (
    <div>
      <ul style={{ listStyle: 'none', margin: 0, padding: 0 }} aria-label="Lịch sử giao dịch tiền cọc">
        {entries.map((entry) => (
          <li key={entry.id} style={{ padding: '8px 0', borderBottom: '1px solid var(--color-border)' }}>
            <Flex justify="space-between" gap={12} className="dms-wrap">
              <Typography.Text strong style={{ color: entry.amount < 0 ? '#B91C1C' : '#15803D' }}>
                {formatSignedMoney(entry.amount)}
              </Typography.Text>
              <span style={{ textAlign: 'right' }}>{entry.description}</span>
            </Flex>
            <Typography.Text type="secondary">{formatDate(entry.date)}</Typography.Text>
          </li>
        ))}
      </ul>
      <Flex justify="space-between" style={{ paddingTop: 12 }}>
        <Typography.Text strong>Số dư</Typography.Text>
        <Typography.Text strong>{formatMoney(balance)}</Typography.Text>
      </Flex>
    </div>
  )
}
