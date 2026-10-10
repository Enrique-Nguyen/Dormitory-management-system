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
    <div style={{ display: 'flex', gap: 12, padding: '12px 0' }} className="dms-wrap">
      <span aria-hidden="true" style={{ color: isRead ? '#9CA3AF' : '#1D4ED8' }}>
        {isRead ? '○' : '●'}
      </span>
      <div>
        {!isRead && <span className="sr-only">Chưa đọc: </span>}
        <div style={{ fontWeight: isRead ? 400 : 600 }}>{title}</div>
        <div>{body}</div>
        <div style={{ color: '#6B7280', fontSize: 12 }}>{formatRelative(createdAt)}</div>
      </div>
    </div>
  )

  if (link) {
    return (
      <Link to={link} onClick={onClick} style={{ display: 'block', color: 'inherit' }}>
        {content}
      </Link>
    )
  }
  return onClick ? (
    <button type="button" onClick={onClick} style={{ display: 'block', width: '100%', textAlign: 'left', background: 'none', border: 0, font: 'inherit', cursor: 'pointer' }}>
      {content}
    </button>
  ) : (
    content
  )
}
