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
      description={
        <div>
          <div style={{ fontWeight: 600 }}>{title}</div>
          {description && <div style={{ color: '#6B7280' }}>{description}</div>}
        </div>
      }
    >
      {action}
    </Empty>
  )
}
