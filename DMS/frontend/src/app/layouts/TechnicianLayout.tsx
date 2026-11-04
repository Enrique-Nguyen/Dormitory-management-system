import { Permission } from '../../constants/authorization'
import { Flex, Layout } from 'antd'
import { Link, Outlet } from 'react-router'
import NotificationBell from '../../components/layout/NotificationBell'
import UserMenu from '../../components/layout/UserMenu'
import { useMe } from '../../features/account/useMe'
import { hasPermission } from '../../services/api/auth'
import ThemeSwitcher from '../../components/layout/ThemeSwitcher'

/** Cổng KTV: tối giản, không sidebar, hành động chính lớn full-width (.dms-action-lg) (UI 4.3, 22). */
export default function TechnicianLayout() {
  const { data: me } = useMe()
  return (
    <Layout className="dms-touch" style={{ minHeight: '100vh' }}>
      <Layout.Header className="dms-topbar">
        <Link to="/technician/tasks" className="dms-brand-logo">
          <span className="dms-brand-icon">TLU</span>
          <span>DMS</span>
          <span className="dms-portal-tag">Kỹ thuật</span>
        </Link>
        <div style={{ flex: 1 }} />
        <Flex align="center" gap={4}>
          <ThemeSwitcher />
          {hasPermission(me, Permission.NOTIFICATION_READ) && (
            <NotificationBell to="/technician/notifications" />
          )}
          <UserMenu />
        </Flex>
      </Layout.Header>
      <Layout.Content>
        <main className="dms-page" style={{ maxWidth: 640 }}>
          <Outlet />
        </main>
      </Layout.Content>
    </Layout>
  )
}
