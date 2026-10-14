import { Empty } from 'antd'
import type { ReactNode } from 'react'

interface Props {
  title: ReactNode
  description?: ReactNode
  /** Nút hoặc link gợi ý bước tiếp theo (Rule 06: nói rõ phải làm gì). */
  action?: ReactNode
}

export default function EmptyState({ title, description, action }: Props) {
  return (
    <Empty
      image={Empty.PRESENTED_IMAGE_SIMPLE}
      style={{ margin: '24px 0' }}
      description={
        <div>
          <div style={{ fontWeight: 600, fontSize: 15, color: 'var(--color-heading)' }}>{title}</div>
          {description && <div style={{ color: 'var(--color-text-secondary)', fontSize: 13, marginTop: 4 }}>{description}</div>}
        </div>
      }
    >
      {action}
    </Empty>
  )
}
