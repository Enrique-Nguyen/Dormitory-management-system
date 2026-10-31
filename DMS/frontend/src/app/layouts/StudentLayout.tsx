import { Permission } from '../../constants/authorization'
import { Button, Flex, Layout, Menu, Tooltip } from 'antd'
import { MoonOutlined, SunOutlined } from '@ant-design/icons'
import { Link, Outlet, useLocation } from 'react-router'
import NotificationBell from '../../components/layout/NotificationBell'
import UserMenu from '../../components/layout/UserMenu'
import { useMe } from '../../features/account/useMe'
import {
  findActivePath,
  STUDENT_MAINTENANCE_PATH,
  studentBottomNav,
  studentMoreNav,
} from '../../constants/navigation'
import { useViewport } from '../../hooks/useViewport'
import { hasAnyPermission, hasPermission } from '../../services/api/auth'
import { useAppTheme } from '../themeStore'

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
  const { mode, toggleMode } = useAppTheme()
  const visibleNav = allNav.filter((item) => hasAnyPermission(me, item.permissions))
  const visibleMoreNav = moreNav.filter((item) => hasAnyPermission(me, item.permissions))
  const active = findActivePath(visibleNav, pathname)
  // Tab Báo hỏng luôn mở được, nhưng chỉ dùng khi đang ở; nếu không trang sẽ giải thích lý do (UI 4.1)
  const canReport = me?.student?.hasActiveContract ?? false

  return (
    <Layout className="dms-touch" style={{ minHeight: '100vh' }}>
      <Layout.Header className="dms-topbar">
        <Link to="/student" className="dms-brand-logo">
          <span className="dms-brand-icon">TLU</span>
          <span>DMS</span>
          <span className="dms-portal-tag">Sinh viên</span>
        </Link>
        {!isMobile && (
          <nav aria-label="Menu chính" style={{ flex: 1, minWidth: 0, margin: '0 16px' }}>
            <Menu
              mode="horizontal"
              selectedKeys={active ? [active] : []}
              items={visibleNav.map((item) => ({
                key: item.path,
                label: <Link to={item.path}>{item.label}</Link>,
              }))}
              style={{ borderBottom: 0, background: 'transparent' }}
            />
          </nav>
        )}
        {isMobile && <div style={{ flex: 1 }} />}
        <Flex align="center" gap={4}>
          <Tooltip
            title={
              mode === 'dark'
                ? 'Chuyển sang Giao diện Sáng'
                : 'Chuyển sang Giao diện Tối'
            }
          >
            <Button
              type="text"
              shape="circle"
              icon={mode === 'dark' ? <SunOutlined /> : <MoonOutlined />}
              onClick={toggleMode}
              style={{
                width: 36,
                height: 36,
                color: mode === 'dark' ? '#fbbf24' : 'var(--color-text-secondary)',
              }}
              aria-label="Đổi chế độ sáng tối"
            />
          </Tooltip>
          {hasPermission(me, Permission.NOTIFICATION_READ) && (
            <NotificationBell to="/student/notifications" />
          )}
          <UserMenu extraItems={isMobile ? visibleMoreNav : []} />
        </Flex>
      </Layout.Header>

      <Layout.Content className={isMobile ? 'dms-with-bottom-nav' : undefined}>
        <main className="dms-page" style={{ maxWidth: 960 }}>
          <Outlet />
        </main>
      </Layout.Content>

      {isMobile && (
        <nav className="dms-bottom-nav" aria-label="Điều hướng chính">
          {studentBottomNav
            .filter((item) => hasAnyPermission(me, item.permissions))
            .map((item) => {
              const isActive = active === item.path
              const limited = item.path === STUDENT_MAINTENANCE_PATH && !canReport
              return (
                <Link
                  key={item.path}
                  to={item.path}
                  aria-current={isActive ? 'page' : undefined}
                  data-limited={limited}
                >
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
