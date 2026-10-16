import { Card } from '../components/ui/Surface'
import { Descriptions } from 'antd'
import { useLocation } from 'react-router'
import EmptyState from '../components/ui/EmptyState'
import PageHeader from '../components/layout/PageHeader'

interface Props {
  title: string
  /** Thành viên phụ trách theo UI 27.3 ("TV3"). */
  owner: string
  /** Tham chiếu để làm: mục UI / mã FR. */
  spec: string
}

/**
 * Trang giữ chỗ. Khi làm màn thật, thay `element` trong app/router/<role>Routes.tsx bằng trang của bạn
 * (đặt ở pages/<role>/) và dùng PageHeader + components/ cho đồng bộ.
 */
export default function PlaceholderPage({ title, owner, spec }: Props) {
  const { pathname } = useLocation()
  return (
    <>
      <PageHeader title={title} />
      <Card>
        <EmptyState
          title="Màn hình này chưa được dựng"
          description="Khung layout, guard và route đã sẵn sàng."
        />
        <Descriptions column={1} size="small" style={{ marginTop: 16 }}>
          <Descriptions.Item label="Phụ trách">{owner}</Descriptions.Item>
          <Descriptions.Item label="Tham chiếu">{spec}</Descriptions.Item>
          <Descriptions.Item label="Đường dẫn">
            <code>{pathname}</code>
          </Descriptions.Item>
        </Descriptions>
      </Card>
    </>
  )
}
