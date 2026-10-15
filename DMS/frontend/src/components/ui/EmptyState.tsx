import { Empty } from 'antd'
import type { ReactNode } from 'react'
import { commonUiLabels } from '../../constants/navigation'

interface Props {
  title?: ReactNode
  description?: ReactNode
  /** Nút hoặc link gợi ý bước tiếp theo (Rule 06: nói rõ phải làm gì). */
  action?: ReactNode
}

export default function EmptyState({
  title = commonUiLabels.emptyData,
  description,
  action,
}: Props) {
  return (
    <Empty
      image={Empty.PRESENTED_IMAGE_SIMPLE}
      className="dms-empty"
      description={
        <div>
          <div className="dms-empty-title">{title}</div>
          {description && (
            <div className="dms-empty-description">{description}</div>
          )}
        </div>
      }
    >
      {action}
    </Empty>
  )
}
