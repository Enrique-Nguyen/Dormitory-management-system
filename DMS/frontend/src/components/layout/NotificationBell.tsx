import { BellOutlined } from '@ant-design/icons'
import { Badge } from 'antd'
import { Link } from 'react-router'
import { useUnreadCount } from '../../features/notification/useUnreadCount'

/** Chuông trên topbar, kèm số chưa đọc; bấm mở trang thông báo của cổng (`to`). */
export default function NotificationBell({ to }: { to: string }) {
  const { data: count = 0 } = useUnreadCount()
  return (
    <Link
      to={to}
      aria-label={count > 0 ? `Thông báo, ${count} chưa đọc` : 'Thông báo'}
      style={{ display: 'inline-flex', alignItems: 'center', justifyContent: 'center', minWidth: 44, minHeight: 44, color: 'inherit' }}
    >
      <Badge count={count} size="small" overflowCount={99}>
        <BellOutlined style={{ fontSize: 20 }} aria-hidden="true" />
      </Badge>
    </Link>
  )
}
