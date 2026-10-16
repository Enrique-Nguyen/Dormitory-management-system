import { Link } from 'react-router'
import { formatRelative } from '../../utils/format'

interface Props {
  title: string
  body: string
  createdAt: string
  isRead: boolean
  /** Đường dẫn frontend do backend suy ra (API Spec 4.17); null với thông báo Staff đăng. */
  link?: string | null
  onClick?: () => void
}

/** Một dòng thông báo (UI mục 24). Chưa đọc: chấm ● + chữ ẩn "Chưa đọc" cho trình đọc màn hình. */
export default function NotificationItem({ title, body, createdAt, isRead, link, onClick }: Props) {
  const content = (
    <div
      className={`dms-notification-item dms-wrap ${!isRead ? 'dms-notification-item--unread' : ''}`}
      style={{ borderBottom: '1px solid var(--color-border-light)' }}
    >
      <span
        aria-hidden="true"
        className={`dms-notification-dot ${isRead ? 'dms-notification-dot--read' : 'dms-notification-dot--unread'}`}
      />
      <div style={{ flex: 1, minWidth: 0 }}>
        {!isRead && <span className="sr-only">Chưa đọc: </span>}
        <div style={{ fontWeight: isRead ? 500 : 700, fontSize: 14, color: isRead ? 'var(--color-text)' : 'var(--color-heading)' }}>
          {title}
        </div>
        <div style={{ color: 'var(--color-text-secondary)', fontSize: 13, marginTop: 2 }}>{body}</div>
        <div style={{ color: 'var(--color-text-muted)', fontSize: 12, marginTop: 4 }}>
          {formatRelative(createdAt)}
        </div>
      </div>
    </div>
  )

  if (link) {
    return (
      <Link to={link} onClick={onClick} style={{ display: 'block', color: 'inherit', textDecoration: 'none' }}>
        {content}
      </Link>
    )
  }
  return onClick ? (
    <button
      type="button"
      onClick={onClick}
      style={{
        display: 'block',
        width: '100%',
        textAlign: 'left',
        background: 'none',
        border: 0,
        padding: 0,
        font: 'inherit',
        cursor: 'pointer',
      }}
    >
      {content}
    </button>
  ) : (
    content
  )
}
