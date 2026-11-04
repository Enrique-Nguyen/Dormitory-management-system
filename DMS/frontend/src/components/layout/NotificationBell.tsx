import { BellOutlined, CheckOutlined } from '@ant-design/icons'
import { Button, Popover, Spin, Tooltip } from 'antd'
import { useState } from 'react'
import { useNavigate } from 'react-router'
import { Badge } from '../ui/Surface'
import {
  useInbox,
  useMarkAllRead,
  useMarkRead,
  useUnreadCount,
} from '../../features/notification'
import type { NotificationResponse } from '../../services/api/notification'
import { formatRelative } from '../../utils/format'

const POPUP_PAGE_SIZE = 5

/** Chuông trên topbar: bấm mở dropdown danh sách thông báo, cuối danh sách có nút "Xem chi tiết" mở màn CRUD (`to`). */
export default function NotificationBell({ to }: { to: string }) {
  const [open, setOpen] = useState(false)
  const navigate = useNavigate()
  const { data: count = 0 } = useUnreadCount()
  const markRead = useMarkRead()
  const markAllRead = useMarkAllRead()

  const inbox = useInbox({
    page: 0,
    size: POPUP_PAGE_SIZE,
    sort: ['createdAt,desc'],
  })

  const notifications = inbox.data?.content ?? []

  function handleItemClick(item: NotificationResponse) {
    if (!item.isRead) {
      markRead.mutate(item.id)
    }
    setOpen(false)
    if (item.link) {
      navigate(item.link)
    }
  }

  const popoverContent = (
    <div style={{ width: 360, maxWidth: 'calc(100vw - 32px)' }}>
      {/* Header */}
      <div
        style={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          padding: '12px 16px',
          borderBottom: '1px solid var(--color-border-light)',
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
          <span
            style={{
              fontSize: 15,
              fontWeight: 700,
              color: 'var(--color-heading)',
            }}
          >
            Thông báo
          </span>
          {count > 0 && (
            <span
              style={{
                fontSize: 11,
                fontWeight: 600,
                color: 'var(--color-primary)',
                background: 'var(--color-primary-light)',
                padding: '1px 6px',
                borderRadius: 10,
              }}
            >
              {count} mới
            </span>
          )}
        </div>
        {count > 0 && (
          <Tooltip title="Đánh dấu tất cả là đã đọc">
            <Button
              type="text"
              size="small"
              icon={<CheckOutlined />}
              loading={markAllRead.isPending}
              onClick={() => markAllRead.mutate()}
              style={{
                fontSize: 12,
                color: 'var(--color-primary)',
                padding: '0 4px',
              }}
            >
              Đã đọc tất cả
            </Button>
          </Tooltip>
        )}
      </div>

      {/* Body: Danh sách thông báo */}
      <div style={{ maxHeight: 340, overflowY: 'auto' }}>
        {inbox.isLoading ? (
          <div style={{ padding: '32px 16px', textAlign: 'center' }}>
            <Spin size="small" />
          </div>
        ) : !notifications.length ? (
          <div
            style={{
              padding: '36px 16px',
              textAlign: 'center',
              color: 'var(--color-text-muted)',
            }}
          >
            <BellOutlined
              style={{
                fontSize: 28,
                opacity: 0.35,
                display: 'block',
                margin: '0 auto 8px',
              }}
            />
            <div style={{ fontSize: 13, fontWeight: 500 }}>
              Chưa có thông báo nào
            </div>
          </div>
        ) : (
          notifications.map((item) => (
            <div
              key={item.id}
              onClick={() => handleItemClick(item)}
              role="button"
              tabIndex={0}
              onKeyDown={(e) => {
                if (e.key === 'Enter' || e.key === ' ') {
                  handleItemClick(item)
                }
              }}
              style={{
                display: 'flex',
                gap: 10,
                alignItems: 'flex-start',
                padding: '10px 14px',
                borderBottom: '1px solid var(--color-border-light)',
                cursor: 'pointer',
                background: item.isRead
                  ? 'transparent'
                  : 'var(--color-primary-light, #eff6ff)',
                transition: 'background 0.2s',
              }}
              className="dms-notification-popover-item"
            >
              <span
                style={{
                  width: 8,
                  height: 8,
                  borderRadius: '50%',
                  marginTop: 6,
                  flexShrink: 0,
                  background: item.isRead
                    ? 'var(--color-border-strong, #cbd5e1)'
                    : 'var(--color-primary, #1d4ed8)',
                  boxShadow: item.isRead
                    ? undefined
                    : '0 0 0 3px var(--color-primary-light)',
                }}
              />
              <div style={{ flex: 1, minWidth: 0 }}>
                <div
                  style={{
                    fontSize: 13,
                    fontWeight: item.isRead ? 500 : 700,
                    color: item.isRead
                      ? 'var(--color-text)'
                      : 'var(--color-heading)',
                    whiteSpace: 'nowrap',
                    overflow: 'hidden',
                    textOverflow: 'ellipsis',
                  }}
                >
                  {item.title}
                </div>
                <div
                  style={{
                    fontSize: 12,
                    color: 'var(--color-text-secondary)',
                    marginTop: 2,
                    display: '-webkit-box',
                    WebkitLineClamp: 2,
                    WebkitBoxOrient: 'vertical',
                    overflow: 'hidden',
                    lineHeight: 1.4,
                  }}
                >
                  {item.body}
                </div>
                <div
                  style={{
                    fontSize: 11,
                    color: 'var(--color-text-muted)',
                    marginTop: 4,
                  }}
                >
                  {formatRelative(item.createdAt)}
                </div>
              </div>
            </div>
          ))
        )}
      </div>

      {/* Footer: Nút xem chi tiết ra màn CRUD */}
      <div
        style={{
          padding: '10px 14px',
          borderTop: '1px solid var(--color-border-light)',
          background: 'var(--color-surface)',
          textAlign: 'center',
        }}
      >
        <Button
          type="primary"
          ghost
          block
          onClick={() => {
            setOpen(false)
            navigate(to)
          }}
          style={{
            fontWeight: 600,
            height: 34,
            fontSize: 13,
            borderRadius: 6,
          }}
        >
          Xem chi tiết
        </Button>
      </div>
    </div>
  )

  return (
    <Popover
      open={open}
      onOpenChange={setOpen}
      trigger="click"
      placement="bottomRight"
      arrow={{ pointAtCenter: true }}
      overlayInnerStyle={{ padding: 0, borderRadius: 12, overflow: 'hidden' }}
      content={popoverContent}
    >
      <Button
        type="text"
        shape="circle"
        aria-label={count > 0 ? `Thông báo, ${count} chưa đọc` : 'Thông báo'}
        style={{
          width: 36,
          height: 36,
          display: 'inline-flex',
          alignItems: 'center',
          justifyContent: 'center',
          color: open ? 'var(--color-primary)' : 'var(--color-text-secondary)',
          background: open ? 'var(--color-surface-hover)' : 'transparent',
        }}
      >
        <Badge count={count} size="small" overflowCount={99}>
          <BellOutlined style={{ fontSize: 18 }} aria-hidden="true" />
        </Badge>
      </Button>
    </Popover>
  )
}
