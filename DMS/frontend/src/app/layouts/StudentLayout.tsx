import { Layout, Menu } from 'antd'
import { Link, Outlet, useLocation } from 'react-router'
import NotificationBell from '../../components/layout/NotificationBell'
import UserMenu from '../../components/layout/UserMenu'
import { useMe } from '../../features/account/useMe'
import { findActivePath, STUDENT_MAINTENANCE_PATH, studentBottomNav, studentMoreNav } from '../../constants/navigation'
import { useViewport } from '../../hooks/useViewport'

// Thông báo đã có chuông trên header nên không lặp trong menu
const moreNav = studentMoreNav.filter((item) => item.path !== '/student/notifications')
const allNav = [...studentBottomNav, ...moreNav]

/**
 * Cổng sinh viên, mobile-first (UI 4.1). Dưới 640 px: header + bottom nav 4 tab, các mục còn lại nằm trong menu avatar.
 * Từ 640 px: menu ngang trên header.
 */
export default function StudentLayout() {
  const viewport = useViewport()
  const isMobile = viewport === 'mobile'
  const { pathname } = useLocation()
  const { data: me } = useMe()
  const active = findActivePath(allNav, pathname)
  // Tab Báo hỏng luôn mở được, nhưng chỉ dùng khi đang ở; nếu không trang sẽ giải thích lý do (UI 4.1)
  const canReport = me?.student?.hasActiveContract ?? false

  return (
    <Layout className="dms-touch" style={{ minHeight: '100vh' }}>
      <Layout.Header style={{ position: 'sticky', top: 0, zIndex: 10, display: 'flex', alignItems: 'center', gap: 12, borderBottom: '1px solid var(--color-border)' }}>
        <Link to="/student" style={{ fontWeight: 700, fontSize: 18, color: '#1D4ED8' }}>
          DMS
        </Link>
        {!isMobile && (
          <nav aria-label="Menu chính" style={{ flex: 1, minWidth: 0 }}>
            <Menu
              mode="horizontal"
              selectedKeys={active ? [active] : []}
              items={allNav.map((item) => ({ key: item.path, label: <Link to={item.path}>{item.label}</Link> }))}
              style={{ borderBottom: 0 }}
            />
          </nav>
        )}
        {isMobile && <div style={{ flex: 1 }} />}
        <NotificationBell to="/student/notifications" />
        <UserMenu extraItems={isMobile ? moreNav : []} />
      </Layout.Header>

      <Layout.Content className={isMobile ? 'dms-with-bottom-nav' : undefined}>
        <main className="dms-page" style={{ maxWidth: 960 }}>
          <Outlet />
        </main>
      </Layout.Content>

      {isMobile && (
        <nav className="dms-bottom-nav" aria-label="Điều hướng chính">
          {studentBottomNav.map((item) => {
            const isActive = active === item.path
            const limited = item.path === STUDENT_MAINTENANCE_PATH && !canReport
            return (
              <Link key={item.path} to={item.path} aria-current={isActive ? 'page' : undefined} data-limited={limited}>
                {item.icon && <item.icon />}
                <span>{item.label}</span>
                {limited && <span className="sr-only">(chỉ dùng khi đang ở KTX)</span>}
              </Link>
            )
          })}
        </nav>
      )}
    </Layout>
  )
}
