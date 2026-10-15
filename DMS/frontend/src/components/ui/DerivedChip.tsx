import { Tag } from 'antd'
import {
  derivedLabels,
  derivedToneColors,
  type DerivedLabel,
} from '../../constants/statusConfig'

interface Props {
  label: DerivedLabel
}

/** Chip viền + icon cho nhãn suy ra (Sắp hết hạn, Quá hạn...). Đặt cạnh StatusBadge, không thay thế nó. */
export default function DerivedChip({ label }: Props) {
  const { label: text, tone, icon: Icon } = derivedLabels[label]
  const color = derivedToneColors[tone]
  return (
    <Tag
      variant="outlined"
      icon={<Icon aria-hidden />}
      style={{
        color,
        borderColor: color,
        background: 'var(--color-surface)',
        marginInlineEnd: 0,
      }}
    >
      {text}
    </Tag>
  )
}
