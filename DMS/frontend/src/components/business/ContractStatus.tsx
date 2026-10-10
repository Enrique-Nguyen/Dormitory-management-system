import { Flex } from 'antd'
import type { DerivedLabel } from '../../constants/statusConfig'
import DerivedChip from '../ui/DerivedChip'
import StatusBadge from '../ui/StatusBadge'

interface Props {
  status: string
  /** `labels` do backend tính (BR-08): EXPIRING, OVERDUE_MOVE_OUT, SUMMER_LEAVE. Ngưỡng nằm ở system_config, không tính ở UI. */
  labels?: DerivedLabel[]
}

/** Badge trạng thái hợp đồng kèm các nhãn suy ra: luôn đặt cạnh nhau, nhãn suy ra không thay badge. */
export default function ContractStatus({ status, labels = [] }: Props) {
  return (
    <Flex wrap gap={8} align="center">
      <StatusBadge entity="contract" status={status} />
      {labels.map((label) => (
        <DerivedChip key={label} label={label} />
      ))}
    </Flex>
  )
}
