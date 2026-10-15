import { Card } from './Surface'
import { Skeleton } from 'antd'

/** Khung chờ cho trang chi tiết/danh sách trong lúc query đang tải. */
export default function PageSkeleton({ rows = 4 }: { rows?: number }) {
  return (
    <Card aria-busy="true" aria-label="Đang tải">
      <Skeleton active paragraph={{ rows }} />
    </Card>
  )
}
