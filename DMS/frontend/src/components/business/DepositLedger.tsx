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
          <li key={entry.id} className="dms-ledger-item" style={{ borderBottom: '1px solid var(--color-border-light)' }}>
            <Flex justify="space-between" align="center" gap={12} className="dms-wrap">
              <Typography.Text strong style={{ fontSize: 15, color: entry.amount < 0 ? '#B91C1C' : '#15803D' }}>
                {formatSignedMoney(entry.amount)}
              </Typography.Text>
              <span style={{ textAlign: 'right', fontWeight: 500 }}>{entry.description}</span>
            </Flex>
            <div style={{ color: 'var(--color-text-muted)', fontSize: 12, marginTop: 2 }}>
              {formatDate(entry.date)}
            </div>
          </li>
        ))}
      </ul>
      <div className="dms-balance-callout">
        <div>
          <span style={{ fontSize: 13, color: 'var(--color-text-secondary)', display: 'block' }}>Số dư hiện tại</span>
          <strong style={{ fontSize: 18, color: 'var(--color-heading)' }}>{formatMoney(balance)}</strong>
        </div>
        <span style={{ fontSize: 12, color: 'var(--color-primary)', fontWeight: 500, background: '#FFFFFF', padding: '4px 10px', borderRadius: 'var(--radius-full)', border: '1px solid #BFDBFE' }}>
          Đã bảo đảm
        </span>
      </div>
    </div>
  )
}
