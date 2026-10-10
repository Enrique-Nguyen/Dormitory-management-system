import { Tag } from 'antd'
import { getStatus, statusColors, type StatusEntity } from '../../constants/statusConfig'

interface Props {
  entity: StatusEntity
  status: string
}

/** Badge đặc cho trạng thái lưu trong CSDL: màu + icon + nhãn tiếng Việt (Rule 03). */
export default function StatusBadge({ entity, status }: Props) {
  const { label, color, icon: Icon } = getStatus(entity, status)
  return (
    <Tag
      variant="solid"
      color={statusColors[color]}
      icon={<Icon aria-hidden />}
      style={{ marginInlineEnd: 0 }}
    >
      {label}
    </Tag>
  )
}
