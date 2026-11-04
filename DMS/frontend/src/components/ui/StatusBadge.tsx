import { Tag } from 'antd'
import { getStatus, type StatusEntity } from '../../constants/statusConfig'

interface Props {
  entity: StatusEntity
  status: string
}

export default function StatusBadge({ entity, status }: Props) {
  const { label, color } = getStatus(entity, status)
  return (
    <Tag
      className={`dms-status-badge dms-status-${color}`}
      color={color === 'gray' ? 'default' : color}
      style={{ marginInlineEnd: 0 }}
    >
      {label}
    </Tag>
  )
}
